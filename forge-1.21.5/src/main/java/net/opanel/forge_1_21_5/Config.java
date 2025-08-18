package net.opanel.forge_1_21_5;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> ACCESS_KEY = BUILDER.define("accessKey", "123456");
    private static final ForgeConfigSpec.IntValue WEB_SERVER_PORT = BUILDER.defineInRange("webServerPort", 3000, 1, 65535);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String accessKey;
    public static int webServerPort;

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        accessKey = ACCESS_KEY.get();
        webServerPort = WEB_SERVER_PORT.get();
    }
}
