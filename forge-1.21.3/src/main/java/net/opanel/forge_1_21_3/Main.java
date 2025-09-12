package net.opanel.forge_1_21_3;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.opanel.OPanel;
import net.opanel.forge_1_21_3.command.OPanelCommand;
import net.opanel.forge_1_21_3.config.Config;
import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import net.opanel.forge_1_21_3.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

@Mod(Main.MODID)
@OnlyIn(Dist.DEDICATED_SERVER)
public class Main {
    public static final String MODID = "opanel";
    public static final Logger LOGGER = LogUtils.getLogger();
    public OPanel instance;

    private LogListenerManagerImpl logListenerAppender;

    public Main(FMLJavaModLoadingContext ctx) {
        ctx.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);

        initLogListenerAppender();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        instance = new OPanel(new ConfigManagerImpl(), new LoggerImpl(LOGGER));
        instance.setLogListenerManager(logListenerAppender);

        OPanelCommand.instance = instance;
    }

    private void initLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logListenerAppender = LogListenerManagerImpl.createAppender("LogListenerAppender", true);
        logListenerAppender.start();
        logger.addAppender(logListenerAppender);
    }

    private void disposeLogListenerAppender() {
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.removeAppender(logListenerAppender);
        logListenerAppender.clearListeners();
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        if(instance == null) throw new NullPointerException("OPanel is not initialized.");

        instance.setServer(new ForgeServer(event.getServer()));

        try {
            instance.getWebServer().start(); // default port 3000
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
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

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent evnet) {
        if(instance != null) {
            instance.onTick();
        }
    }

    // ConfigManagerImpl内部类
    private static class ConfigManagerImpl implements ConfigManager {
        @Override
        public OPanelConfiguration get() {
            // 检查是否是首次启动（配置文件中没有accessKey或为默认值）
            String currentAccessKey = Config.ACCESS_KEY.get();
            if (currentAccessKey == null || currentAccessKey.equals(OPanelConfiguration.defaultConfig.accessKey)) {
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
            int webServerPort = Config.WEB_SERVER_PORT.get();
            System.out.println("======Opanel=======");
            System.out.println("url: http://localhost:" + webServerPort);
            System.out.println("Opanel已启动");
            System.out.println("===================");
            
            return new OPanelConfiguration(
                    currentAccessKey,
                    Config.SALT.get(),
                    webServerPort
            );
        }

        @Override
        public void set(OPanelConfiguration config) {
            Config.ACCESS_KEY.set(config.accessKey);
            Config.SALT.set(config.salt);
            Config.WEB_SERVER_PORT.set(config.webServerPort);
            // 不保存明文密码到配置文件，仅在控制台输出
        }
    }
}
