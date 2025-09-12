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
            // 输出明文密码给用户
            System.out.println("[OPanel] 首次启动检测到，已生成随机密码: " + randomConfig.plainPassword);
            System.out.println("[OPanel] 请保存此密码，用于Web面板登录！");
            return randomConfig;
        }
        return config;
    }

    @Override
    public void set(OPanelConfiguration config) {
        // 创建不包含明文密码的配置对象
        OPanelConfiguration configToSave = new OPanelConfiguration(
                config.accessKey,
                config.salt,
                config.webServerPort
        );
        configSrc.set(configToSave);
        // 不保存明文密码到配置文件，仅在控制台输出
    }
}
