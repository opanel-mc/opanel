package net.opanel;

public class OPanelConfiguration {
    public static final OPanelConfiguration defaultConfig = new OPanelConfiguration(
            "123456",
            3000,
            3010
    );

    public String accessKey;
    public int webServerPort;
    public int apiServerPort;

    public OPanelConfiguration(
            String accessKey,
            int webServerPort,
            int apiServerPort
    ) {
        this.accessKey = accessKey;
        this.webServerPort = webServerPort;
        this.apiServerPort = apiServerPort;
    }
}
