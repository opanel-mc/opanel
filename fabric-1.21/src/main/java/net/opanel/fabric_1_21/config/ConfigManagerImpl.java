package net.opanel.fabric_1_21.config;

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
        if (config.accessKey == null || "your-access-key".equals(config.accessKey)) {
            // 生成随机配置
            OPanelConfiguration randomConfig = OPanelConfiguration.createRandomConfig();
            // 保存配置
            set(randomConfig);
            return randomConfig;
        }
        return config;
    }

    @Override
    public void set(OPanelConfiguration config) {
        configSrc.set(config);
    }
}
