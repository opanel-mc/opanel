package net.opanel.neoforge_1_21_1.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    @Override
    public OPanelConfiguration get() {
        return new OPanelConfiguration(
                Config.ACCESS_KEY.get(),
                Config.SALT.get(),
                Config.WEB_SERVER_PORT.get(),
                Config.COOKIE_SECURE.get(),
                Config.PROXY_HEADERS.get()
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        Config.ACCESS_KEY.set(config.accessKey);
        Config.SALT.set(config.salt);
        Config.WEB_SERVER_PORT.set(config.webServerPort);
        Config.COOKIE_SECURE.set(config.cookieSecure);
        Config.PROXY_HEADERS.set(config.proxyHeaders);
        Config.SPEC.save();
    }
}
