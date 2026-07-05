package net.opanel.neoforge_helper.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.opanel.config.OPanelConfiguration;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> ACCESS_KEY = BUILDER.define("accessKey", OPanelConfiguration.defaultConfig.accessKey);
    public static final ModConfigSpec.ConfigValue<String> SALT = BUILDER.define("salt", OPanelConfiguration.defaultConfig.salt);
    public static final ModConfigSpec.IntValue WEB_SERVER_PORT = BUILDER.defineInRange("webServerPort", OPanelConfiguration.defaultConfig.webServerPort, 1, 65535);
    public static final ModConfigSpec.IntValue MCDR_SOCKET_PORT = BUILDER.defineInRange("mcdrSocketPort", OPanelConfiguration.defaultConfig.mcdrSocketPort, 1, 65535);
    public static final ModConfigSpec.BooleanValue COOKIE_SECURE = BUILDER.define("cookieSecure", OPanelConfiguration.defaultConfig.cookieSecure);
    public static final ModConfigSpec.BooleanValue PROXY_HEADERS = BUILDER.define("proxyHeaders", OPanelConfiguration.defaultConfig.proxyHeaders);
    public static final ModConfigSpec.BooleanValue OIDC_ENABLED = BUILDER.define("oidcEnabled", OPanelConfiguration.defaultConfig.oidcEnabled);
    public static final ModConfigSpec.ConfigValue<String> OIDC_DISCOVERY_URL = BUILDER.define("oidcDiscoveryUrl", OPanelConfiguration.defaultConfig.oidcDiscoveryUrl);
    public static final ModConfigSpec.ConfigValue<String> OIDC_CLIENT_ID = BUILDER.define("oidcClientId", OPanelConfiguration.defaultConfig.oidcClientId);
    public static final ModConfigSpec.ConfigValue<String> OIDC_CLIENT_SECRET = BUILDER.define("oidcClientSecret", OPanelConfiguration.defaultConfig.oidcClientSecret);
    public static final ModConfigSpec.ConfigValue<String> OIDC_DISPLAY_NAME = BUILDER.define("oidcDisplayName", OPanelConfiguration.defaultConfig.oidcDisplayName);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
