package net.opanel.forge_helper.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    @Override
    public OPanelConfiguration get() {
        return new OPanelConfiguration(
                Config.ACCESS_KEY.get(),
                Config.SALT.get(),
                Config.WEB_SERVER_HOST.get(),
                Config.WEB_SERVER_PORT.get(),
                Config.MCDR_SOCKET_PORT.get(),
                Config.MAP_PRERENDER_CONCURRENT.get(),
                Config.MONITOR_SNAPSHOT_INTERVAL.get(),
                Config.SERVER_RESTART_DELAY.get(),
                Config.COOKIE_SECURE.get(),
                Config.PROXY_HEADERS.get(),
                Config.OIDC_ENABLED.get(),
                Config.OIDC_DISCOVERY_URL.get(),
                Config.OIDC_CLIENT_ID.get(),
                Config.OIDC_CLIENT_SECRET.get(),
                Config.OIDC_DISPLAY_NAME.get()
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        Config.ACCESS_KEY.set(config.accessKey);
        Config.SALT.set(config.salt);
        Config.WEB_SERVER_HOST.set(config.webServerHost);
        Config.WEB_SERVER_PORT.set(config.webServerPort);
        Config.MCDR_SOCKET_PORT.set(config.mcdrSocketPort);
        Config.MAP_PRERENDER_CONCURRENT.set(config.mapPrerenderConcurrent);
        Config.MONITOR_SNAPSHOT_INTERVAL.set(config.monitorSnapshotInterval);
        Config.SERVER_RESTART_DELAY.set(config.serverRestartDelay);
        Config.COOKIE_SECURE.set(config.cookieSecure);
        Config.PROXY_HEADERS.set(config.proxyHeaders);
        Config.OIDC_ENABLED.set(config.oidcEnabled);
        Config.OIDC_DISCOVERY_URL.set(config.oidcDiscoveryUrl);
        Config.OIDC_CLIENT_ID.set(config.oidcClientId);
        Config.OIDC_CLIENT_SECRET.set(config.oidcClientSecret);
        Config.OIDC_DISPLAY_NAME.set(config.oidcDisplayName);
    }
}
