package net.opanel.forge_1_21_5;

import com.mojang.logging.LogUtils;
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
import net.opanel.OPanelConfiguration;
import net.opanel.forge_1_21_5.command.OPanelCommand;
import net.opanel.forge_1_21_5.terminal.LogListenerManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

@Mod(Main.MODID)
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
        instance = new OPanel(new OPanelConfiguration(Config.accessKey, Config.webServerPort), new LoggerImpl(LOGGER));
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
        if(logListenerAppender != null) disposeLogListenerAppender();
        if(instance != null) instance.stop();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent evnet) {
        if(instance == null) throw new NullPointerException("OPanel is not initialized.");

        instance.onTick();
    }
}
