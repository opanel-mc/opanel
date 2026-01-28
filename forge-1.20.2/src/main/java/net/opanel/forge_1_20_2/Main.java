package net.opanel.forge_1_20_2;

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
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.opanel.OPanel;
import net.opanel.forge_1_20_2.command.OPanelCommand;
import net.opanel.forge_1_20_2.terminal.LogListenerManagerImpl;
import net.opanel.forge_helper.InventorySerializer;
import net.opanel.forge_helper.InventorySyncTask;
import net.opanel.forge_helper.config.Config;
import net.opanel.forge_helper.config.ConfigManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

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

    public Main() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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
        InventorySerializer.setResolver(new Forge1202ItemDataResolver());
        inventorySyncTask = new InventorySyncTask(player -> new ForgePlayer(player), 20);
        forgeListener = new ForgeListener();
        forgeListener.setInventorySyncTask(inventorySyncTask);
        MinecraftForge.EVENT_BUS.register(forgeListener);
    }

    private void disposeLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.removeAppender(logListenerAppender);
        logListenerAppender.clearListeners();
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        if(instance == null) { LOGGER.error("OPanel not initialized."); return; }
        this.server = event.getServer();
        instance.setServer(new ForgeServer(server));
        try { instance.getWebServer().start(); } catch (Exception e) { LOGGER.error("Failed to start web server: " + e.getMessage()); }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        try { if(logListenerAppender != null) disposeLogListenerAppender(); } catch(Exception e) { LOGGER.error(e.getMessage()); }
        try { if(instance != null) instance.stop(); } catch(Exception e) { LOGGER.error(e.getMessage()); }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (instance != null) instance.onTick();
        if (inventorySyncTask != null && server != null) inventorySyncTask.onTick(server);
    }
}
