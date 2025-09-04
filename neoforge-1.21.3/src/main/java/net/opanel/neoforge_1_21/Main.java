package net.opanel.neoforge_1_21;

import com.mojang.logging.LogUtils;
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
import net.opanel.neoforge_1_21.command.OPanelCommand;
import net.opanel.neoforge_1_21.config.Config;
import net.opanel.neoforge_1_21.config.ConfigManagerImpl;
import net.opanel.neoforge_1_21.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

@Mod(value = Main.MODID, dist = Dist.DEDICATED_SERVER)
public class Main {
    public static final String MODID = "opanel";
    public static final Logger LOGGER = LogUtils.getLogger();
    public OPanel instance;

    private LogListenerManagerImpl logListenerAppender;

    public Main(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        NeoForge.EVENT_BUS.register(this);

        initLogListenerAppender();
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

    private void disposeLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.removeAppender(logListenerAppender);
        logListenerAppender.clearListeners();
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        if(instance == null) throw new NullPointerException("OPanel is not initialized.");

        instance.setServer(new NeoServer(event.getServer()));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        if(logListenerAppender != null) disposeLogListenerAppender();
        if(instance != null) instance.stop();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post evnet) {
        if(instance == null) throw new NullPointerException("OPanel is not initialized.");

        instance.onTick();
    }
}
