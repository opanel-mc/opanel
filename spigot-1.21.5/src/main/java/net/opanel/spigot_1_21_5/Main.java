package net.opanel.spigot_1_21_5;

import de.tr7zw.changeme.nbtapi.NBT;
import net.opanel.OPanel;
import net.opanel.OPanelConfiguration;
import net.opanel.spigot_1_21_5.terminal.LogListenerManagerImpl;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener {
    public final Logger LOGGER = getLogger();
    public OPanel instance;

    private BukkitTask serverTickListener;

    @Override
    public void onEnable() {
        if(!NBT.preloadApi()) {
            LOGGER.warning("Cannot start OPanel plugin: NBT-API is not initialized properly.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        final LoggerImpl logger = new LoggerImpl(LOGGER);

        saveDefaultConfig();
        FileConfiguration configSrc = getConfig();
        final OPanelConfiguration config = new OPanelConfiguration(
                configSrc.getString("accessKey"),
                configSrc.getInt("webServerPort")
        );

        instance = new OPanel(config, logger);

        initLogHandler();
        initServerTickListener();

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if(serverTickListener != null) serverTickListener.cancel();
        if(instance != null) instance.stop();
    }

    private void initLogHandler() {
        final LogListenerManagerImpl logHandler = new LogListenerManagerImpl();
        LOGGER.addHandler(logHandler);
        instance.setLogListenerManager(logHandler);
    }

    private void initServerTickListener() {
        serverTickListener = Bukkit.getScheduler().runTaskTimer(this, instance::onTick, 0L, 1L);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        instance.setServer(new SpigotServer(getServer()));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}