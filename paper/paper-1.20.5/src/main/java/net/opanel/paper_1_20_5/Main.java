package net.opanel.paper_1_20_5;

import com.cozooo.dlc_fileops_helper.FileOpsHelperBootstrap;
import de.tr7zw.changeme.nbtapi.NBT;
import net.opanel.OPanel;
import net.opanel.paper_helper.TaskRunner;
import net.opanel.paper_helper.command.OPanelCommand;
import net.opanel.paper_helper.config.ConfigManagerImpl;
import net.opanel.common.Constants;
import net.opanel.paper_1_20_5.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener, TaskRunner {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
    public final Logger LOGGER = getLogger();
    public OPanel instance;

    private BukkitTask serverTickListener;
    private LogListenerManagerImpl logListenerAppender;

    @Override
    public void onEnable() {
        if(!NBT.preloadApi()) {
            LOGGER.warning("Cannot start OPanel plugin: NBT-API is not initialized properly.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        FileOpsHelperBootstrap.initialize(Paths.get(""), OPanel.TMP_DIR_PATH);

        final LoggerImpl logger = new LoggerImpl(LOGGER);

        saveDefaultConfig();
        instance = new OPanel(new ConfigManagerImpl(getConfig(), this), logger);

        initLogListenerAppender();
        initServerTickListener();

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new PaperListener(this), this);

        getCommand("opanel").setExecutor(new OPanelCommand(instance));

        // bStats
        new Metrics(this, Constants.BSTATS_ID);
    }

    @Override
    public void onDisable() {
        try {
            if(logListenerAppender != null) disposeLogListenerAppender();
        } catch(Exception e) {
            log.error("Failed to dispose log listener appender: " + e.getMessage());
        }
        
        try {
            if(serverTickListener != null && !serverTickListener.isCancelled()) {
                serverTickListener.cancel();
            }
        } catch(Exception e) {
            log.error("Failed to cancel server tick listener: " + e.getMessage());
        }
        
        try {
            if(instance != null) instance.stop();
        } catch(Exception e) {
            log.error("Failed to stop OPanel instance: " + e.getMessage());
        }
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
        instance.setServer(new PaperServer(this, getServer()));

        try {
            instance.getWebServer().start(); // default port 3000
            instance.getMapRenderManager().init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void runTask(Runnable task) {
        Object lock = new Object();
        synchronized(lock) {
            Bukkit.getScheduler().runTask(this, () -> {
                task.run();
                synchronized(lock) {
                    lock.notify();
                }
            });
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
