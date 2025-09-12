package net.opanel.config;

import net.opanel.utils.Utils;

public class OPanelConfiguration {
    public static final OPanelConfiguration defaultConfig = createRandomConfig();

    public String accessKey;
    public String salt;
    public int webServerPort;
    public String plainPassword; // 用于存储明文密码，仅在首次启动时使用

    public OPanelConfiguration(
            String accessKey,
            String salt,
            int webServerPort
    ) {
        this.accessKey = accessKey;
        this.salt = salt;
        this.webServerPort = webServerPort;
        this.plainPassword = null;
    }

    public OPanelConfiguration(
            String accessKey,
            String salt,
            int webServerPort,
            String plainPassword
    ) {
        this.accessKey = accessKey;
        this.salt = salt;
        this.webServerPort = webServerPort;
        this.plainPassword = plainPassword;
    }

    /**
     * 创建带有随机密码的配置
     * @return 新的配置实例
     */
    public static OPanelConfiguration createRandomConfig() {
        String randomPassword = Utils.generateRandomPassword(12);
        String hashedPassword = Utils.md5(Utils.md5(randomPassword)); // 双重哈希
        return new OPanelConfiguration(
                hashedPassword,
                "opanel",
                3000,
                randomPassword
        );
    }
}
