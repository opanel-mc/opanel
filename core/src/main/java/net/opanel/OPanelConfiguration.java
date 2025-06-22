package net.opanel;

public class OPanelConfiguration {
    public static final OPanelConfiguration defaultConfig = new OPanelConfiguration(
            "123456",
            3000
    );

    public String accessKey;
    public int webServerPort;

    public OPanelConfiguration(
            String accessKey,
            int webServerPort
    ) {
        this.accessKey = accessKey;
        this.webServerPort = webServerPort;
    }
}
