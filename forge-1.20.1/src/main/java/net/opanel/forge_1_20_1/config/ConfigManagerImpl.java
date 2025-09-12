package net.opanel.forge_1_20_1.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    @Override
    public OPanelConfiguration get() {
        // 检查是否是首次启动（配置文件中没有accessKey或为默认值）
        String currentAccessKey = Config.ACCESS_KEY.get();
        if (currentAccessKey == null || currentAccessKey.equals(OPanelConfiguration.defaultConfig.accessKey)) {
            // 首次启动，生成随机配置
            OPanelConfiguration randomConfig = OPanelConfiguration.createRandomConfig();
            set(randomConfig); // 保存到配置文件
            return randomConfig;
        }
        
        return new OPanelConfiguration(
                currentAccessKey,
                Config.SALT.get(),
                Config.WEB_SERVER_PORT.get()
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        Config.ACCESS_KEY.set(config.accessKey);
        Config.SALT.set(config.salt);
        Config.WEB_SERVER_PORT.set(config.webServerPort);
    }
}
