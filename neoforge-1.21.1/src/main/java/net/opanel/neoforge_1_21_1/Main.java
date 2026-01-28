package net.opanel.neoforge_1_21_1;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.opanel.OPanel;
import net.opanel.neoforge_1_21_1.command.OPanelCommand;
import net.opanel.neoforge_1_21_1.config.Config;
import net.opanel.neoforge_1_21_1.config.ConfigManagerImpl;
import net.opanel.neoforge_1_21_1.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

/**
 * Main entry point for the OPanel NeoForge 1.21.1 mod.
 * 
 * THREAD SAFETY:
 * - All ServerTickEvent.Post callbacks run on the main server thread
 * - Server lifecycle events run on the main server thread
 * - NeoInventorySyncTask.onTick() MUST be called from the main thread
 */
@Mod(value = Main.MODID, dist = Dist.DEDICATED_SERVER)
public class Main {
    public static final String MODID = "opanel";
    public static final Logger LOGGER = LogUtils.getLogger();
    public OPanel instance;

    private LogListenerManagerImpl logListenerAppender;
    private NeoListener neoListener;
    private NeoInventorySyncTask inventorySyncTask;
    private MinecraftServer server;

    public Main(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        NeoForge.EVENT_BUS.register(this);
        
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
        NeoInventorySerializer.setResolver(new Neo1211ItemDataResolver());
        
        // Create sync task with player factory
        inventorySyncTask = new NeoInventorySyncTask(
            NeoPlayer::new,
            20 // Sync every 20 ticks (1 second)
        );
        
        // Create listener and link sync task
        neoListener = new NeoListener();
        neoListener.setInventorySyncTask(inventorySyncTask);
        
        // Register listener with event bus
        NeoForge.EVENT_BUS.register(neoListener);
    }

    private void disposeLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.removeAppender(logListenerAppender);
        logListenerAppender.clearListeners();
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        if(instance == null) throw new NullPointerException("OPanel is not initialized.");

        this.server = event.getServer();
        instance.setServer(new NeoServer(server));

        try {
            instance.getWebServer().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        try {
            if(logListenerAppender != null) disposeLogListenerAppender();
        } catch (Exception e) {
            LOGGER.error("Failed to dispose log listener appender: " + e.getMessage());
        }
        
        try {
            if(instance != null) instance.stop();
        } catch (Exception e) {
            LOGGER.error("Failed to stop OPanel instance: " + e.getMessage());
        }
    }

    /**
     * Called every server tick on the MAIN THREAD (Post phase).
     * Safe to access player inventories and emit events.
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if(instance == null) throw new NullPointerException("OPanel is not initialized.");

        instance.onTick();
        
        // Run inventory sync task (main thread - safe for inventory access)
        if (inventorySyncTask != null && server != null) {
            inventorySyncTask.onTick(server);
        }
    }
}

