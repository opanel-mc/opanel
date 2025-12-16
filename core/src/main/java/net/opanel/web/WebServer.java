package net.opanel.web;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import io.javalin.http.HttpStatus;
import io.javalin.jetty.JettyServer;
import io.javalin.json.JavalinGson;
import io.javalin.util.JavalinLogger;
import net.opanel.OPanel;
import net.opanel.api.*;
import net.opanel.terminal.TerminalEndpoint;

import java.io.IOException;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebServer {
    public final int PORT;

    private final OPanel plugin;
    private Javalin app;

    public WebServer(OPanel plugin) {
        this.plugin = plugin;
        PORT = plugin.getConfig().webServerPort;

        JavalinLogger.enabled = false;
    }

    public void start() throws Exception {
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;

            // Gson configuration
            config.jsonMapper(new JavalinGson(new Gson()));

            // CORS configuration
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.allowHost("http://localhost:3001"); // for dev
                    it.allowCredentials = true;
                });
            });

            // Multipart configuration
            config.jetty.multipartConfig.cacheDirectory(OPanel.TMP_DIR_PATH.toString());
            config.jetty.multipartConfig.maxInMemoryFileSize(10, SizeUnit.MB);

            // Frontend
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/web";
            });
        });

        // Websocket
        app.ws("/terminal", ws -> new TerminalEndpoint(ws, plugin));

        // Controllers
        BeforeController beforeController = new BeforeController(plugin);
        ErrorController errorController = new ErrorController(plugin);
        DownloadController downloadController = new DownloadController(plugin);
        AuthController authController = new AuthController(plugin);
        BannedIpsController bannedIpsController = new BannedIpsController(plugin);
        ControlController controlController = new ControlController(plugin);
        GamerulesController gamerulesController = new GamerulesController(plugin);
        IconController iconController = new IconController(plugin);
        InfoController infoController = new InfoController(plugin);
        LogsController logsController = new LogsController(plugin);
        MonitorController monitorController = new MonitorController(plugin);
        PlayersController playersController = new PlayersController(plugin);
        SavesController savesController = new SavesController(plugin);
        SecurityController securityController = new SecurityController(plugin);
        VersionController versionController = new VersionController(plugin);
        WhitelistController whitelistController = new WhitelistController(plugin);

        // API Routes
        app.before("/*", beforeController.beforeAll);
        app.before("/*", beforeController.handleRsc);
        app.before("/*", beforeController.handleFonts);
        app.routes(() -> path("file", () -> {
            before("/*", beforeController.authCookie);
            get("/{id}/{fileName}", downloadController.downloadFile);
        }));
        app.routes(() -> path("api", () -> {
            before("/*", beforeController.authCookie);

            path("auth", () -> {
                get("/", authController.getCram);
                post("/", authController.validateCram);
            });
            path("banned-ips", () -> {
                get("/", bannedIpsController.getBannedIps);
                post("add", bannedIpsController.banIp);
                post("remove", bannedIpsController.pardonIp);
            });
            path("control", () -> {
                get("properties", controlController.getServerProperties);
                post("properties", controlController.setServerProperties);
                get("code-of-conduct", controlController.getCodeOfConducts);
                post("code-of-conduct", controlController.changeCodeOfConduct);
                delete("code-of-conduct", controlController.removeCodeOfConduct);
                post("stop", controlController.stopServer);
                post("reload", controlController.reloadServer);
                post("world", controlController.switchSave);
            });
            path("gamerules", () -> {
                get("/", gamerulesController.getGamerules);
                post("/", gamerulesController.changeGamerule);
            });
            path("icon", () -> {
                get("/", iconController.getFavicon);
                post("/", iconController.uploadFavicon);
            });
            path("info", () -> {
                get("/", infoController.getServerInfo);
                post("motd", infoController.setMotd);
            });
            path("logs", () -> {
                get("/", logsController.getLogFileList);
                get("{fileName}", logsController.getLogContent);
                get("{fileName}/download", logsController.downloadLog);
                delete("/", logsController.clearLogs);
                delete("{fileName}", logsController.deleteLog);
            });
            get("monitor", monitorController.getMonitor);
            path("players", () -> {
                get("/", playersController.getPlayers);
                delete("/", playersController.deletePlayerData);
                post("op", playersController.giveOp);
                post("deop", playersController.depriveOp);
                post("kick", playersController.kickPlayer);
                post("ban", playersController.banPlayer);
                post("pardon", playersController.pardonPlayer);
                post("gamemode", playersController.setGamemode);
            });
            path("saves", () -> {
                get("/", savesController.getSaves);
                post("/", savesController.uploadSave);
                get("{saveName}", savesController.downloadSave);
                post("{saveName}", savesController.editSave);
                delete("{saveName}", savesController.deleteSave);
            });
            post("security", securityController.updateAccessKey);
            get("version", versionController.getVersionInfo);
            path("whitelist", () -> {
                get("/", whitelistController.getWhitelist);
                post("enable", whitelistController.enableWhitelist);
                post("disable", whitelistController.disableWhitelist);
                post("write", whitelistController.writeWhitelist);
                post("add", whitelistController.addWhitelistEntry);
                post("remove", whitelistController.removeWhitelistEntry);
            });
        }));

        // Not found page
        app.error(HttpStatus.NOT_FOUND, errorController.notFound);

        app.start(PORT);
        plugin.logger.info("OPanel web server is ready on port "+ PORT);
        plugin.initializeAccessKey();

        app.events(event -> {
            event.serverStopping(() -> {
                try {
                    TerminalEndpoint.closeAllSessions();
                } catch (IOException e) {
                    plugin.logger.error("Failed to close WebSocket sessions: " + e.getMessage());
                }
            });
        });
    }

    public void stop() throws Exception {
        if(isRunning()) {
            app.stop();
            app = null;
            plugin.logger.info("Web server is stopped.");
        }
    }

    public boolean isRunning() {
        if(app == null) return false;

        JettyServer jettyServer = app.jettyServer();
        return jettyServer != null && jettyServer.started;
    }
}
