package net.opanel.forge_1_20_2.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.opanel.config.OPanelConfiguration;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> ACCESS_KEY = BUILDER.define("accessKey", OPanelConfiguration.defaultConfig.accessKey);
    public static final ForgeConfigSpec.ConfigValue<String> SALT = BUILDER.define("salt", OPanelConfiguration.defaultConfig.salt);
    public static final ForgeConfigSpec.IntValue WEB_SERVER_PORT = BUILDER.defineInRange("webServerPort", OPanelConfiguration.defaultConfig.webServerPort, 1, 65535);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
