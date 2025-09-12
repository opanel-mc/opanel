package net.opanel.folia_1_20;

import de.tr7zw.changeme.nbtapi.NBT;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.opanel.OPanel;
import net.opanel.folia_1_20.command.OPanelCommand;
import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import net.opanel.folia_1_20.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener {
    public static final boolean isPaper;
    public static final boolean isFolia;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
    public final Logger LOGGER = getLogger();
    public OPanel instance;

    private ScheduledTask serverTickListener;
    private LogListenerManagerImpl logListenerAppender;

    static {
        boolean _isPaper;
        boolean _isFolia;
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            _isPaper = true;
        } catch (ClassNotFoundException e) {
            _isPaper = false;
        }
        
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            _isFolia = true;
        } catch (ClassNotFoundException e) {
            _isFolia = false;
        }
        
        isPaper = _isPaper;
        isFolia = _isFolia;
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
        if (isFolia) {
            // Use Folia's global region scheduler for tick events
            serverTickListener = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, 
                (task) -> instance.onTick(), 1, 1);
        } else {
            // Fallback to Bukkit scheduler for non-Folia servers
            Bukkit.getScheduler().runTaskTimer(this, instance::onTick, 0L, 1L);
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        instance.setServer(new FoliaServer(this, getServer()));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runTask(Runnable task) {
        if (isFolia) {
            // Use Folia's global region scheduler for async tasks
            Bukkit.getGlobalRegionScheduler().run(this, (scheduledTask) -> task.run());
        } else {
            // Fallback to Bukkit scheduler
            Bukkit.getScheduler().runTask(this, task);
        }
    }
    
    public void runTaskLater(Runnable task, long delay) {
        if (isFolia) {
            // Use Folia's global region scheduler with delay
            Bukkit.getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delay);
        } else {
            // Fallback to Bukkit scheduler
            Bukkit.getScheduler().runTaskLater(this, task, delay);
        }
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