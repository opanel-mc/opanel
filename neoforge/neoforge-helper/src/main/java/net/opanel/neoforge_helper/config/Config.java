package net.opanel.neoforge_helper.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.opanel.config.OPanelConfiguration;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> ACCESS_KEY = BUILDER.define("accessKey", OPanelConfiguration.defaultConfig.accessKey);
    public static final ModConfigSpec.ConfigValue<String> SALT = BUILDER.define("salt", OPanelConfiguration.defaultConfig.salt);
    public static final ModConfigSpec.ConfigValue<String> WEB_SERVER_HOST = BUILDER.define("webServerHost", OPanelConfiguration.defaultConfig.webServerHost);
    public static final ModConfigSpec.IntValue WEB_SERVER_PORT = BUILDER.defineInRange("webServerPort", OPanelConfiguration.defaultConfig.webServerPort, 1, 65535);
    public static final ModConfigSpec.IntValue MCDR_SOCKET_PORT = BUILDER.defineInRange("mcdrSocketPort", OPanelConfiguration.defaultConfig.mcdrSocketPort, 1, 65535);
    public static final ModConfigSpec.IntValue MAP_PRERENDER_CONCURRENT = BUILDER.defineInRange("mapPrerenderConcurrent", OPanelConfiguration.defaultConfig.mapPrerenderConcurrent, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue MONITOR_SNAPSHOT_INTERVAL = BUILDER.defineInRange("monitorSnapshotInterval", OPanelConfiguration.defaultConfig.monitorSnapshotInterval, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SERVER_RESTART_DELAY = BUILDER.defineInRange("serverRestartDelay", OPanelConfiguration.defaultConfig.serverRestartDelay, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue COOKIE_SECURE = BUILDER.define("cookieSecure", OPanelConfiguration.defaultConfig.cookieSecure);
    public static final ModConfigSpec.BooleanValue PROXY_HEADERS = BUILDER.define("proxyHeaders", OPanelConfiguration.defaultConfig.proxyHeaders);
    public static final ModConfigSpec.BooleanValue OIDC_ENABLED = BUILDER.define("oidcEnabled", OPanelConfiguration.defaultConfig.oidcEnabled);
    public static final ModConfigSpec.ConfigValue<String> OIDC_DISCOVERY_URL = BUILDER.define("oidcDiscoveryUrl", OPanelConfiguration.defaultConfig.oidcDiscoveryUrl);
    public static final ModConfigSpec.ConfigValue<String> OIDC_CLIENT_ID = BUILDER.define("oidcClientId", OPanelConfiguration.defaultConfig.oidcClientId);
    public static final ModConfigSpec.ConfigValue<String> OIDC_CLIENT_SECRET = BUILDER.define("oidcClientSecret", OPanelConfiguration.defaultConfig.oidcClientSecret);
    public static final ModConfigSpec.ConfigValue<String> OIDC_DISPLAY_NAME = BUILDER.define("oidcDisplayName", OPanelConfiguration.defaultConfig.oidcDisplayName);
    public static final ModConfigSpec.BooleanValue AUTO_CHECK_PLUGIN_UPDATES = BUILDER.define("autoCheckPluginUpdates", OPanelConfiguration.defaultConfig.autoCheckPluginUpdates);
    public static final ModConfigSpec.IntValue PLUGIN_UPDATE_CHECK_INTERVAL = BUILDER.defineInRange("pluginUpdateCheckInterval", OPanelConfiguration.defaultConfig.pluginUpdateCheckInterval, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue AUTO_APPLY_PLUGIN_UPDATES = BUILDER.define("autoApplyPluginUpdates", OPanelConfiguration.defaultConfig.autoApplyPluginUpdates);
    public static final ModConfigSpec.ConfigValue<String> PLUGIN_UPDATE_RESTART_STRATEGY = BUILDER.define("pluginUpdateRestartStrategy", OPanelConfiguration.defaultConfig.pluginUpdateRestartStrategy);
    public static final ModConfigSpec.ConfigValue<String> CURSEFORGE_API_KEY = BUILDER.define("curseForgeApiKey", OPanelConfiguration.defaultConfig.curseForgeApiKey);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
