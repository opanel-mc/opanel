package net.opanel.fabric_1_21_5;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.opanel.*;
import net.opanel.fabric_1_21_5.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.nocp.configx.api.*;

public class Main implements DedicatedServerModInitializer {
    public static final String MODID = "opanel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public OPanel instance;

    private LogListenerManagerImpl logListenerAppender;

    @Override
    public void onInitializeServer() {
        Configuration<OPanelConfiguration> configSrc = ConfigManager.get().register(MODID, OPanelConfiguration.defaultConfig, OPanelConfiguration.class);
        instance = new OPanel(configSrc.get(), new LoggerImpl(LOGGER));

        initLogListenerAppender();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
    }

    private void initLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logListenerAppender = LogListenerManagerImpl.createAppender("LogListenerAppender", true);
        logListenerAppender.start();
        logger.addAppender(logListenerAppender);
        instance.setLogListenerManager(logListenerAppender);
    }

    private void disposeLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.removeAppender(logListenerAppender);
        logListenerAppender.clearListeners();
    }

    private void onServerStart(MinecraftServer server) {
        instance.setServer(new FabricServer(server));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onServerStop(MinecraftServer server) {
        if(logListenerAppender != null) disposeLogListenerAppender();
        if(instance != null) instance.stop();
    }

    private void onServerTick(MinecraftServer server) {
        instance.onTick();
    }
}
