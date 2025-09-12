package net.opanel.spigot_1_20.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    private final FileConfiguration configSrc;

    public ConfigManagerImpl(FileConfiguration configSrc) {
        this.configSrc = configSrc;
    }

    @Override
    public OPanelConfiguration get() {
        // 检查是否是首次启动（配置文件中没有accessKey或为默认值）
        String accessKey = configSrc.getString("accessKey");
        if (!configSrc.contains("accessKey") || accessKey == null || "your-access-key".equals(accessKey)) {
            // 生成随机配置
            OPanelConfiguration randomConfig = OPanelConfiguration.createRandomConfig();
            // 保存配置
            set(randomConfig);
            return randomConfig;
        }
        
        return new OPanelConfiguration(
                configSrc.getString("accessKey"),
                configSrc.getString("salt"),
                configSrc.getInt("webServerPort")
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        configSrc.set("accessKey", config.accessKey);
        configSrc.set("salt", config.salt);
        configSrc.set("webServerPort", config.webServerPort);
    }
}
