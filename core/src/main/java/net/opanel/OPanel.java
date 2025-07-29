package net.opanel;

import net.opanel.terminal.LogListenerManager;
import net.opanel.common.OPanelServer;
import net.opanel.logger.Loggable;
import net.opanel.utils.TPS;
import net.opanel.web.WebServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OPanel {
    public static final Path OPANEL_DIR_PATH = Paths.get("").resolve(".opanel");
    public static final Path TMP_DIR_PATH = OPANEL_DIR_PATH.resolve("tmp");

    private final OPanelConfiguration config;
    public final Loggable logger;

    private WebServer webServer;
    private OPanelServer server;
    private LogListenerManager logListenerManager;

    public OPanel(OPanelConfiguration config, Loggable logger) {
        this.config = config;
        this.logger = logger;

        // Initialize
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Setup web server
        webServer = new WebServer(this);
    }

    private void init() throws IOException {
        File opanelDir = new File(OPANEL_DIR_PATH.toString());
        if(!opanelDir.exists() && !opanelDir.mkdir()) {
            throw new IOException("Cannot initialize .opanel directory.");
        }
        File tmpDir = new File(TMP_DIR_PATH.toString());
        if(!tmpDir.exists() && !tmpDir.mkdir()) {
            throw new IOException("Cannot initialize .opanel/tmp directory.");
        }
    }

    public void onTick() {
        TPS.onTick();
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
