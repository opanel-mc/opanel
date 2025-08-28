package net.opanel.forge_1_21_5.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    @Override
    public OPanelConfiguration get() {
        return new OPanelConfiguration(
                Config.accessKey,
                Config.salt,
                Config.webServerPort
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        Config.accessKey = config.accessKey;
        Config.salt = config.salt;
        Config.webServerPort = config.webServerPort;
    }
}
