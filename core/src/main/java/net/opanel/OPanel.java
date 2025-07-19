package net.opanel;

import net.opanel.terminal.LogListenerManager;
import net.opanel.common.OPanelServer;
import net.opanel.logger.Loggable;
import net.opanel.web.WebServer;

public class OPanel {
    private final OPanelConfiguration config;
    public final Loggable logger;

    private WebServer webServer;
    private OPanelServer server;
    private LogListenerManager logListenerManager;

    public OPanel(OPanelConfiguration config, Loggable logger) {
        this.config = config;
        this.logger = logger;

        // Setup web server
        webServer = new WebServer(this);
    }

    public OPanelConfiguration getConfig() {
        return config;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public void setServer(OPanelServer server) {
        this.server = server;
    }

    public OPanelServer getServer() {
        return server;
    }

    public void setLogListenerManager(LogListenerManager manager) {
        logListenerManager = manager;
    }

    public LogListenerManager getLogListenerManager() {
        return logListenerManager;
    }

    public void stop() {
        try {
            webServer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
