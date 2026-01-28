package net.opanel.fabric_1_21;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.opanel.*;
import net.opanel.config.OPanelConfiguration;
import net.opanel.fabric_1_21.command.OPanelCommand;
import net.opanel.fabric_1_21.terminal.LogListenerManagerImpl;
import net.opanel.fabric_helper.InventorySerializer;
import net.opanel.fabric_helper.InventorySyncTask;
import net.opanel.fabric_helper.config.ConfigManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.nocp.configx.api.*;

/**
 * Main entry point for the OPanel Fabric 1.21 mod.
 * 
 * THREAD SAFETY:
 * - All ServerTickEvents callbacks run on the main server thread
 * - ServerLifecycleEvents callbacks run on the main server thread
 * - InventorySyncTask.onTick() MUST be called from the main thread
 */
public class Main implements DedicatedServerModInitializer {
    public static final String MODID = "opanel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public OPanel instance;

    private LogListenerManagerImpl logListenerAppender;
    private FabricListener fabricListener;
    private InventorySyncTask inventorySyncTask;

    @Override
    public void onInitializeServer() {
        Configuration<OPanelConfiguration> configSrc = ConfigManager.get().register(MODID, OPanelConfiguration.defaultConfig, OPanelConfiguration.class);
        instance = new OPanel(new ConfigManagerImpl(configSrc), new LoggerImpl(LOGGER));

        initLogListenerAppender();
        initInventorySync();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        
        // START_SERVER_TICK runs on the main server thread - safe for inventory operations
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        CommandRegistrationCallback.EVENT.register(new OPanelCommand(instance));
    }

    private void initLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logListenerAppender = LogListenerManagerImpl.createAppender("LogListenerAppender", true);
        logListenerAppender.start();
        logger.addAppender(logListenerAppender);
        instance.setLogListenerManager(logListenerAppender);
    }

    private void initInventorySync() {
        // Inject version-specific ItemDataResolver
        InventorySerializer.setResolver(new Fabric121ItemDataResolver());
        
        // Create sync task with player factory
        // The task uses slicing to process 10 players per tick for performance
        inventorySyncTask = new InventorySyncTask(
            player -> new FabricPlayer(player),
            20 // Sync every 20 ticks (1 second)
        );
        
        // Create listener and link sync task
        fabricListener = new FabricListener();
        fabricListener.setInventorySyncTask(inventorySyncTask);
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
            LOGGER.error("Failed to start OPanel web server: " + e.getMessage());
        }
    }

    private void onServerStop(MinecraftServer server) {
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
     * Called every server tick on the MAIN THREAD.
     * Safe to access player inventories and emit events.
     */
    private void onServerTick(MinecraftServer server) {
        if (instance != null) {
            instance.onTick();
        }
        
        // Run inventory sync task (main thread - safe for inventory access)
        if (inventorySyncTask != null) {
            inventorySyncTask.onTick(server);
        }
    }
}
