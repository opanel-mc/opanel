package net.opanel;

import net.opanel.common.Constants;
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
    public static final String JAVALIN_VERSION;
    public static final Path OPANEL_DIR_PATH = Paths.get("").resolve("opanel");
    public static final Path TMP_DIR_PATH = OPANEL_DIR_PATH.resolve(".tmp");
    public static final Path INITIAL_ACCESS_KEY_PATH = OPANEL_DIR_PATH.resolve("INITIAL_ACCESS_KEY.txt");

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
            System.err.println("Failed to load version information: "+ e.getMessage());
        }

        VERSION = version;
    }

    static {
        String version = "unknown";
        try(InputStream is = OPanel.class.getClassLoader().getResourceAsStream("META-INF/maven/io.javalin/javalin/pom.properties")) {
            if(is != null) {
                Properties props = new Properties();
                props.load(is);
                version = props.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            System.err.println("Failed to load javalin version information: "+ e.getMessage());
        }

        JAVALIN_VERSION = version;
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
        File opanelDir = OPANEL_DIR_PATH.toFile();
        if(!opanelDir.exists() && !opanelDir.mkdir()) {
            throw new IOException("Cannot initialize opanel directory.");
        }
        File tmpDir = TMP_DIR_PATH.toFile();
        if(!tmpDir.exists() && !tmpDir.mkdir()) {
            throw new IOException("Cannot initialize opanel/.tmp directory.");
        }
        if(tmpDir.list().length > 0) {
            Utils.clearDirectoryRecursively(tmpDir.toPath());
        }
        File initialAccessKeyFile = INITIAL_ACCESS_KEY_PATH.toFile();
        if(initialAccessKeyFile.exists() && !initialAccessKeyFile.delete()) {
            throw new IOException("Cannot delete opanel/INITIAL_ACCESS_KEY.txt file, please delete it manually for your server security.");
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

    public void initializeAccessKey() { // This method will be called when the web server is ready
        OPanelConfiguration config = getConfig();
        if(config.accessKey.isEmpty()) {
            // Generate access key and then store it into the config
            final String accessKey = Utils.generateRandomCharSequence(12, true);
            config.accessKey = Utils.md5(Utils.md5(accessKey));
            setConfig(config);

            // Store the plaintext access key into the temp file
            try {
                Utils.writeTextFile(INITIAL_ACCESS_KEY_PATH, Constants.INITIAL_ACCESS_KEY_TEMPLATE + accessKey);
            } catch (IOException e) {
                logger.error("Failed to write the initial access key into INITIAL_ACCESS_KEY.txt: "+ e.getMessage());
                throw new RuntimeException("Plaintext initial access key storage failed", e);
            }

            logger.warn("===========================OPanel===========================");
            logger.warn("Initial launching detected,");
            logger.warn("Check opanel/INITIAL_ACCESS_KEY.txt for the initial access key.");
            logger.warn("Remember to delete the file for your server security.");
            logger.warn("============================================================");
        }
        if(config.salt.isEmpty()) {
            // Generate salt and then store it into the config
            config.salt = Utils.generateRandomCharSequence(6, true);
            setConfig(config);
        }
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
        sb.append("§r§7Javalin Version: §f").append(JAVALIN_VERSION);
        return sb.toString();
    }
}
