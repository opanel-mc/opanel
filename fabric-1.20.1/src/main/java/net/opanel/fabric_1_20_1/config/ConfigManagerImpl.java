package net.opanel.fabric_1_20_1.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import space.nocp.configx.api.Configuration;

public class ConfigManagerImpl implements ConfigManager {
    private final Configuration<OPanelConfiguration> configSrc;

    public ConfigManagerImpl(Configuration<OPanelConfiguration> configSrc) {
        this.configSrc = configSrc;
    }

    @Override
    public OPanelConfiguration get() {
        return configSrc.get();
    }

    @Override
    public void set(OPanelConfiguration config) {
        configSrc.set(config);
        configSrc.save();
    }
}
