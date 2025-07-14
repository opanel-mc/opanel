package net.opanel.fabric_1_21_5;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.opanel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.nocp.configx.api.*;

import java.io.IOException;

public class Main implements DedicatedServerModInitializer {
    public static final String MODID = "opanel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public OPanel instance;

    @Override
    public void onInitializeServer() {
        Configuration<OPanelConfiguration> config = ConfigManager.get().register(MODID, OPanelConfiguration.defaultConfig, OPanelConfiguration.class);
        instance = new OPanel(config.get(), new LoggerImpl(LOGGER));

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
    }

    private void onServerStart(MinecraftServer server) {
        instance.setServer(new FabricServer(server));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}