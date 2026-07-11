package net.opanel.config;

public class OPanelConfiguration {
    public static final OPanelConfiguration defaultConfig = new OPanelConfiguration(
//            "14e1b600b1fd579f47433b88e8d85291", // 123456 (hashed 2)
            "", // to be generated on the initial launch
            "", // to be generated on the initial launch
            "0.0.0.0",
            3000,
            25576,
            4,
            false,
            false,
            false,
            "",
            "",
            "",
            "OIDC"
    );

    public String accessKey;
    public String salt;
    public String webServerHost;
    public int webServerPort;
    public int mcdrSocketPort;
    public int mapPrerenderConcurrent;
    public boolean cookieSecure;
    public boolean proxyHeaders;
    public boolean oidcEnabled;
    public String oidcDiscoveryUrl;
    public String oidcClientId;
    public String oidcClientSecret;
    public String oidcDisplayName;

    public OPanelConfiguration(
            String accessKey,
            String salt,
            String webServerHost,
            int webServerPort,
            int mcdrSocketPort,
            int mapPrerenderConcurrent,
            boolean cookieSecure,
            boolean proxyHeaders,
            boolean oidcEnabled,
            String oidcDiscoveryUrl,
            String oidcClientId,
            String oidcClientSecret,
            String oidcDisplayName
    ) {
        this.accessKey = accessKey;
        this.salt = salt;
        this.webServerHost = webServerHost;
        this.webServerPort = webServerPort;
        this.mcdrSocketPort = mcdrSocketPort;
        this.mapPrerenderConcurrent = mapPrerenderConcurrent;
        this.cookieSecure = cookieSecure;
        this.proxyHeaders = proxyHeaders;
        this.oidcEnabled = oidcEnabled;
        this.oidcDiscoveryUrl = oidcDiscoveryUrl;
        this.oidcClientId = oidcClientId;
        this.oidcClientSecret = oidcClientSecret;
        this.oidcDisplayName = oidcDisplayName;
    }
}
