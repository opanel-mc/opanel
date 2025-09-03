package net.opanel.neoforge_1_21_5.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.opanel.config.OPanelConfiguration;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> ACCESS_KEY = BUILDER.define("accessKey", OPanelConfiguration.defaultConfig.accessKey);
    public static final ModConfigSpec.ConfigValue<String> SALT = BUILDER.define("salt", OPanelConfiguration.defaultConfig.salt);
    public static final ModConfigSpec.IntValue WEB_SERVER_PORT = BUILDER.defineInRange("webServerPort", OPanelConfiguration.defaultConfig.webServerPort, 1, 65535);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
