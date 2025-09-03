package net.opanel.spigot_1_21;

import de.tr7zw.changeme.nbtapi.NBT;
import net.opanel.OPanel;
import net.opanel.spigot_1_21.command.OPanelCommand;
import net.opanel.spigot_1_21.config.ConfigManagerImpl;
import net.opanel.spigot_1_21.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener {
    public static final boolean isPaper;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
    public final Logger LOGGER = getLogger();
    public OPanel instance;

    private BukkitTask serverTickListener;
    private LogListenerManagerImpl logListenerAppender;

    static {
        boolean _isPaper;
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            _isPaper = true;
        } catch (ClassNotFoundException e) {
            _isPaper = false;
        }
        isPaper = _isPaper;
    }

    @Override
    public void onEnable() {
        if(!NBT.preloadApi()) {
            LOGGER.warning("Cannot start OPanel plugin: NBT-API is not initialized properly.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        final LoggerImpl logger = new LoggerImpl(LOGGER);

        saveDefaultConfig();
        instance = new OPanel(new ConfigManagerImpl(getConfig()), logger);

        initLogListenerAppender();
        initServerTickListener();

        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("opanel").setExecutor(new OPanelCommand(instance));
    }

    @Override
    public void onDisable() {
        if(logListenerAppender != null) disposeLogListenerAppender();
        if(serverTickListener != null) serverTickListener.cancel();
        if(instance != null) instance.stop();
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

    private void initServerTickListener() {
        serverTickListener = Bukkit.getScheduler().runTaskTimer(this, instance::onTick, 0L, 1L);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        instance.setServer(new SpigotServer(this, getServer()));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(this, task);
    }
}
