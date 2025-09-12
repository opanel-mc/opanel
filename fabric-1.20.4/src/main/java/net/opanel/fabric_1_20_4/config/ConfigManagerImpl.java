package net.opanel.fabric_1_20_4.config;

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
        OPanelConfiguration config = configSrc.get();
        // 检查是否是首次启动（配置文件中没有accessKey或为默认值）
        if (config.accessKey == null || config.accessKey.equals(OPanelConfiguration.defaultConfig.accessKey)) {
            // 首次启动，生成随机配置
            OPanelConfiguration randomConfig = OPanelConfiguration.createRandomConfig();
            set(randomConfig); // 保存到配置文件
            return randomConfig;
        }
        return config;
    }

    @Override
    public void set(OPanelConfiguration config) {
        configSrc.set(config);
    }
}
