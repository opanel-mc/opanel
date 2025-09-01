package net.opanel.forge_1_21_8.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.opanel.config.OPanelConfiguration;
import net.opanel.forge_1_21_8.Main;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> ACCESS_KEY = BUILDER.define("accessKey", OPanelConfiguration.defaultConfig.accessKey);
    private static final ForgeConfigSpec.ConfigValue<String> SALT = BUILDER.define("salt", OPanelConfiguration.defaultConfig.salt);
    private static final ForgeConfigSpec.IntValue WEB_SERVER_PORT = BUILDER.defineInRange("webServerPort", OPanelConfiguration.defaultConfig.webServerPort, 1, 65535);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String accessKey;
    public static String salt;
    public static int webServerPort;

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        accessKey = ACCESS_KEY.get();
        salt = SALT.get();
        webServerPort = WEB_SERVER_PORT.get();
    }

    public static void setAccessKey(String value) {
        ACCESS_KEY.set(value);
    }

    public static void setSalt(String value) {
        SALT.set(value);
    }

    public static void setWebServerPort(int value) {
        WEB_SERVER_PORT.set(value);
    }
}
