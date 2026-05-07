package net.opanel.forge_helper.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    @Override
    public OPanelConfiguration get() {
        return new OPanelConfiguration(
                Config.ACCESS_KEY.get(),
                Config.SALT.get(),
                Config.WEB_SERVER_PORT.get(),
                Config.MCDR_SOCKET_PORT.get(),
                Config.COOKIE_SECURE.get(),
                Config.PROXY_HEADERS.get()
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        Config.ACCESS_KEY.set(config.accessKey);
        Config.SALT.set(config.salt);
        Config.WEB_SERVER_PORT.set(config.webServerPort);
        Config.MCDR_SOCKET_PORT.set(config.mcdrSocketPort);
        Config.COOKIE_SECURE.set(config.cookieSecure);
        Config.PROXY_HEADERS.set(config.proxyHeaders);
    }
}
