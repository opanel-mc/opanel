package net.opanel.spigot_1_21;

import de.tr7zw.changeme.nbtapi.NBT;
import net.opanel.OPanel;
import net.opanel.spigot_1_21.command.OPanelCommand;
import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
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
        instance = new OPanel(new ConfigManagerImpl(getConfig(), this), logger);

        initLogListenerAppender();
        initServerTickListener();

        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("opanel").setExecutor(new OPanelCommand(instance));
    }

    @Override
    public void onDisable() {
        try {
            if(logListenerAppender != null) disposeLogListenerAppender();
        } catch (Exception e) {
            log.error("Failed to dispose log listener appender: " + e.getMessage());
        }
        
        try {
            if(serverTickListener != null && !serverTickListener.isCancelled()) {
                serverTickListener.cancel();
            }
        } catch (Exception e) {
            log.error("Failed to cancel server tick listener: " + e.getMessage());
        }
        
        try {
            if(instance != null) instance.stop();
        } catch (Exception e) {
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
        try {
            final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            logger.removeAppender(logListenerAppender);
            logListenerAppender.clearListeners();
        } catch (Exception e) {
            log.error("Error disposing log listener appender: " + e.getMessage());
        }
    }

    private void initServerTickListener() {
        serverTickListener = Bukkit.getScheduler().runTaskTimer(this, instance::onTick, 0L, 1L);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        try {
            instance.setServer(new SpigotServer(this, getServer()));
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            log.error("Failed to start OPanel web server: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(this, task);
    }

    // ConfigManagerImpl内部类
    private static class ConfigManagerImpl implements ConfigManager {
        private final FileConfiguration configSrc;
        private final JavaPlugin plugin;

        public ConfigManagerImpl(FileConfiguration configSrc, JavaPlugin plugin) {
            this.configSrc = configSrc;
            this.plugin = plugin;
        }

        @Override
        public OPanelConfiguration get() {
            // 检查是否是首次启动（配置文件中没有accessKey或为默认值）
            String accessKey = configSrc.getString("accessKey");
            if (!configSrc.contains("accessKey") || accessKey == null || "your-access-key".equals(accessKey)) {
                // 生成随机配置
                OPanelConfiguration randomConfig = OPanelConfiguration.createRandomConfig();
                // 保存配置
                set(randomConfig);
                // 输出格式化的首次启动信息
                System.out.println("======Opanel=======");
                System.out.println("url: http://localhost:" + randomConfig.webServerPort);
                System.out.println("passwd: " + randomConfig.plainPassword);
                System.out.println("Opanel已启动");
                System.out.println("===================");
                return randomConfig;
            }
            
            // 非首次启动，输出不包含密码的信息
            int webServerPort = configSrc.getInt("webServerPort");
            System.out.println("======Opanel=======");
            System.out.println("url: http://localhost:" + webServerPort);
            System.out.println("Opanel已启动");
            System.out.println("===================");
            
            return new OPanelConfiguration(
                    configSrc.getString("accessKey"),
                    configSrc.getString("salt"),
                    webServerPort
            );
        }

        @Override
        public void set(OPanelConfiguration config) {
            configSrc.set("accessKey", config.accessKey);
            configSrc.set("salt", config.salt);
            configSrc.set("webServerPort", config.webServerPort);
            // 不保存明文密码到配置文件，仅在控制台输出
            plugin.saveConfig(); // 保存配置到磁盘
        }
    }
}
