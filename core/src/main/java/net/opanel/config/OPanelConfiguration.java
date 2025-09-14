package net.opanel.config;

public class OPanelConfiguration {
    public static final OPanelConfiguration defaultConfig = new OPanelConfiguration(
//            "14e1b600b1fd579f47433b88e8d85291", // 123456 (hashed 2)
            "", // to be generated on the initial launch
            "opanel",
            3000
    );

    public String accessKey;
    public String salt;
    public int webServerPort;

    public OPanelConfiguration(
            String accessKey,
            String salt,
            int webServerPort
    ) {
        this.accessKey = accessKey;
        this.salt = salt;
        this.webServerPort = webServerPort;
    }
}
