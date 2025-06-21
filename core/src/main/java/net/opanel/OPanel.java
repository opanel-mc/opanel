package net.opanel;

import net.opanel.api.ApiServer;
import net.opanel.common.OPanelServer;
import net.opanel.logger.Loggable;
import net.opanel.web.WebServer;

public class OPanel {
    private final OPanelConfiguration config;
    public final Loggable logger;

    private WebServer webServer;
    private ApiServer apiServer;

    private OPanelServer server;

    public OPanel(OPanelConfiguration config, Loggable logger) {
        this.config = config;
        this.logger = logger;

        webServer = new WebServer(this);
        apiServer = new ApiServer(this);
    }

    public OPanelConfiguration getConfig() {
        return config;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public ApiServer getApiServer() {
        return apiServer;
    }

    public void setServer(OPanelServer server) {
        this.server = server;
    }

    public OPanelServer getServer() {
        return server;
    }
}
