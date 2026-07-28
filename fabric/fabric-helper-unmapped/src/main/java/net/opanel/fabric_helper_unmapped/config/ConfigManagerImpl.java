package net.opanel.fabric_helper_unmapped.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import net.fabricmc.loader.api.FabricLoader;
import net.opanel.fabric_config.JsonConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    private final JsonConfiguration<OPanelConfiguration> configSrc;

    public ConfigManagerImpl() {
        this.configSrc = new JsonConfiguration<>(
                FabricLoader.getInstance().getConfigDir().resolve("opanel.json"),
                OPanelConfiguration.defaultConfig,
                OPanelConfiguration.class
        );
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
