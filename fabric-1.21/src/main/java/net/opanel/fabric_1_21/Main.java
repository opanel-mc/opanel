package net.opanel.fabric_1_21;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.opanel.*;
import net.opanel.config.OPanelConfiguration;
import net.opanel.fabric_1_21.command.OPanelCommand;
import net.opanel.config.ConfigManager;
import net.opanel.fabric_1_21.terminal.LogListenerManagerImpl;
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
        instance = new OPanel(new ConfigManagerImpl(configSrc), new LoggerImpl(LOGGER));

        initLogListenerAppender();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
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

    private void onServerTick(MinecraftServer server) {
        instance.onTick();
    }

    // ConfigManagerImpl内部类
    private static class ConfigManagerImpl implements ConfigManager {
        private final Configuration<OPanelConfiguration> configSrc;

        public ConfigManagerImpl(Configuration<OPanelConfiguration> configSrc) {
            this.configSrc = configSrc;
        }

        @Override
        public OPanelConfiguration get() {
            OPanelConfiguration config = configSrc.get();
            // 检查是否是首次启动（配置文件中没有accessKey或为默认值）
            if (config.accessKey == null || "your-access-key".equals(config.accessKey)) {
                // 首次启动，生成随机配置
                OPanelConfiguration randomConfig = OPanelConfiguration.createRandomConfig();
                set(randomConfig); // 保存到配置文件
                // 输出格式化的首次启动信息
                System.out.println("======Opanel=======");
                System.out.println("url: http://localhost:" + randomConfig.webServerPort);
                System.out.println("passwd: " + randomConfig.plainPassword);
                System.out.println("Opanel已启动");
                System.out.println("===================");
                return randomConfig;
            }
            
            // 非首次启动，输出不包含密码的信息
            System.out.println("======Opanel=======");
            System.out.println("url: http://localhost:" + config.webServerPort);
            System.out.println("Opanel已启动");
            System.out.println("===================");
            
            return config;
        }

        @Override
        public void set(OPanelConfiguration config) {
            // 创建不包含明文密码的配置对象
            OPanelConfiguration configToSave = new OPanelConfiguration(
                    config.accessKey,
                    config.salt,
                    config.webServerPort
            );
            configSrc.set(configToSave);
            // 不保存明文密码到配置文件，仅在控制台输出
        }
    }
}
