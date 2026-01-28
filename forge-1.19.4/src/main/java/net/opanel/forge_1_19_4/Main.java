package net.opanel.forge_1_19_4;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.opanel.OPanel;
import net.opanel.forge_1_19_4.command.OPanelCommand;
import net.opanel.forge_1_19_4.terminal.LogListenerManagerImpl;
import net.opanel.forge_helper.InventorySerializer;
import net.opanel.forge_helper.InventorySyncTask;
import net.opanel.forge_helper.config.Config;
import net.opanel.forge_helper.config.ConfigManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

/**
 * Main entry point for the OPanel Forge 1.19.4 mod.
 * 
 * THREAD SAFETY:
 * - All TickEvent.ServerTickEvent callbacks run on the main server thread
 * - Server lifecycle events run on the main server thread
 * - InventorySyncTask.onTick() MUST be called from the main thread
 */
@Mod(Main.MODID)
@OnlyIn(Dist.DEDICATED_SERVER)
public class Main {
    public static final String MODID = "opanel";
    public static final Logger LOGGER = LogUtils.getLogger();
    public OPanel instance;

    private LogListenerManagerImpl logListenerAppender;
    private ForgeListener forgeListener;
    private InventorySyncTask inventorySyncTask;
    private MinecraftServer server;

    public Main(FMLJavaModLoadingContext ctx) {
        ctx.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        
        initLogListenerAppender();
        initInventorySync();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        instance = new OPanel(new ConfigManagerImpl(), new LoggerImpl(LOGGER));
        instance.setLogListenerManager(logListenerAppender);

        OPanelCommand.instance = instance;
    }

    private void initLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logListenerAppender = LogListenerManagerImpl.createAppender("LogListenerAppender", true);
        logListenerAppender.start();
        logger.addAppender(logListenerAppender);
    }

    private void initInventorySync() {
        // Inject version-specific ItemDataResolver
        InventorySerializer.setResolver(new Forge1194ItemDataResolver());
        
        // Create sync task with player factory
        inventorySyncTask = new InventorySyncTask(
            player -> new ForgePlayer(player),
            20 // Sync every 20 ticks (1 second)
        );
        
        // Create listener and link sync task
        forgeListener = new ForgeListener();
        forgeListener.setInventorySyncTask(inventorySyncTask);
        
        // Register listener with event bus
        MinecraftForge.EVENT_BUS.register(forgeListener);
    }

    private void disposeLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.removeAppender(logListenerAppender);
        logListenerAppender.clearListeners();
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        if(instance == null) {
            LOGGER.error("OPanel instance is not initialized during server start.");
            return;
        }

        this.server = event.getServer();
        instance.setServer(new ForgeServer(server));

        try {
            instance.getWebServer().start();
        } catch (Exception e) {
            LOGGER.error("Failed to start OPanel web server: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        try {
            if(logListenerAppender != null) disposeLogListenerAppender();
        } catch(Exception e) {
            LOGGER.error("Failed to dispose log listener appender: " + e.getMessage());
        }
        
        try {
            if(instance != null) instance.stop();
        } catch(Exception e) {
            LOGGER.error("Failed to stop OPanel instance: " + e.getMessage());
        }
    }

    /**
     * Called every server tick on the MAIN THREAD.
     * Safe to access player inventories and emit events.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // Only process on END phase to avoid double processing
        if (event.phase != TickEvent.Phase.END) return;
        
        if (instance != null) {
            instance.onTick();
        }
        
        // Run inventory sync task (main thread - safe for inventory access)
        if (inventorySyncTask != null && server != null) {
            inventorySyncTask.onTick(server);
        }
    }
}

