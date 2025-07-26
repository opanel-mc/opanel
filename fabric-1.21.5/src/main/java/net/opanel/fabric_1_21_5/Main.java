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

    @Override
    public void onInitializeServer() {
        Configuration<OPanelConfiguration> config = ConfigManager.get().register(MODID, OPanelConfiguration.defaultConfig, OPanelConfiguration.class);
        instance = new OPanel(config.get(), new LoggerImpl(LOGGER));

        initLogListenerAppender();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
    }

    private void initLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        final LogListenerManagerImpl appender = LogListenerManagerImpl.createAppender("LogListenerAppender", true);
        appender.start();
        logger.addAppender(appender);
        instance.setLogListenerManager(appender);
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
        instance.stop();
    }

    private void onServerTick(MinecraftServer server) {
        instance.onTick();
    }
}
