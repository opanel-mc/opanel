package net.opanel;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import net.opanel.terminal.LogListenerManager;
import net.opanel.common.OPanelServer;
import net.opanel.logger.Loggable;
import net.opanel.utils.TPS;
import net.opanel.utils.Utils;
import net.opanel.web.WebServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class OPanel {
    public static final String VERSION;
    public static final Path OPANEL_DIR_PATH = Paths.get("").resolve(".opanel");
    public static final Path TMP_DIR_PATH = OPANEL_DIR_PATH.resolve("tmp");

    // Read opanel.properties
    static {
        String version = "unknown";
        try(InputStream is = OPanel.class.getClassLoader().getResourceAsStream("opanel.properties")) {
            if(is != null) {
                Properties props = new Properties();
                props.load(is);
                version = props.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            System.err.println("Failed to load version information: " + e.getMessage());
        }

        VERSION = version;
    }

    private final ConfigManager configManager;
    public final Loggable logger;

    private Uptimer uptimer;
    private WebServer webServer;
    private OPanelServer server;
    private LogListenerManager logListenerManager;

    public OPanel(ConfigManager configManager, Loggable logger) {
        this.configManager = configManager;
        this.logger = logger;
        uptimer = new Uptimer();

        // Initialize
        try {
            init();
        } catch (IOException e) {
            logger.error("Failed to initialize OPanel directories: " + e.getMessage());
            throw new RuntimeException("OPanel initialization failed", e);
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
        if(tmpDir.list().length > 0) {
            Utils.clearDirectoryRecursively(tmpDir.toPath());
        }
    }

    public void onTick() {
        TPS.onTick();
    }

    public OPanelConfiguration getConfig() {
        return configManager.get();
    }

    public void setConfig(OPanelConfiguration config) {
        configManager.set(config);
    }

    public Uptimer getUptimer() {
        return uptimer;
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
        if(webServer == null) return;
        try {
            webServer.stop();
        } catch (Exception e) {
            logger.error("Failed to stop web server: " + e.getMessage());
        }
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§lOPanel §r§fStatus\n");
        sb.append("§r§7Version: §f").append(VERSION).append("\n");
        sb.append("§r§7Status: ").append(getWebServer().isRunning() ? "§aRunning" : "§cStopped").append("\n");
        sb.append("§r§7Port: §f").append(getConfig().webServerPort).append("\n");
        sb.append("§r§7Jetty Version: §f").append(getWebServer().getJettyVersion());
        return sb.toString();
    }
}
