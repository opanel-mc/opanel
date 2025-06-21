package net.opanel;

import net.opanel.api.ApiServer;
import net.opanel.logger.Loggable;
import net.opanel.web.WebServer;

import java.io.File;
import java.io.InputStream;

public class OPanel {
    private final OPanelConfiguration config;
    public final Loggable logger;

    private WebServer webServer;
    private ApiServer apiServer;

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
}