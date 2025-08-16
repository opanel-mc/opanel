package net.opanel.bukkit_1_21_5;

import net.opanel.OPanel;
import net.opanel.OPanelConfiguration;
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
        final LoggerImpl logger = new LoggerImpl(LOGGER);

        saveDefaultConfig();
        FileConfiguration configSrc = getConfig();
        final OPanelConfiguration config = new OPanelConfiguration(
                configSrc.getString("accessKey"),
                configSrc.getInt("webServerPort")
        );

        instance = new OPanel(config, logger);

        initLogListenerAppender();
        initServerTickListener();
    }

    @Override
    public void onDisable() {
        serverTickListener.cancel();
        instance.stop();
    }

    /** @todo */
    private void initLogListenerAppender() {

    }

    private void initServerTickListener() {
        serverTickListener = Bukkit.getScheduler().runTaskTimer(this, instance::onTick, 0L, 1L);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        instance.setServer(new BukkitServer(getServer()));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}