package net.opanel.web;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.jetty.JettyServer;
import net.opanel.OPanel;
import net.opanel.api.*;
import net.opanel.terminal.TerminalEndpoint;
import org.eclipse.jetty.util.Jetty;

import java.io.IOException;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebServer {
    public final int PORT;

    private final OPanel plugin;
    private Javalin app;

    public WebServer(OPanel plugin) {
        this.plugin = plugin;
        PORT = plugin.getConfig().webServerPort;
    }

    public void start() throws Exception {
        app = Javalin.create(config -> {
            // CORS configuration
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.allowHost("http://localhost:3001"); // for dev
                    it.exposeHeader("X-Requested-With");
                    it.exposeHeader("Content-Type");
                    it.exposeHeader("X-Credential-Token");
                });
            });

            // Frontend
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/web";
                staticFiles.headers = Map.of("X-Powered-By", "OPanel");
                staticFiles.mimeTypes.add("application/octet-stream", "ttf");
            });
        });

        // Websocket
        app.ws("/terminal", ws -> new TerminalEndpoint(ws, plugin));

        // Authorization
        app.before("/api/*", ctx -> {
            ctx.header("X-Powered-By", "OPanel");

            if(ctx.path().equals("/api/auth") || ctx.path().equals("/api/icon")) return;

            String token = ctx.header("X-Credential-Token"); // jws
            if(token == null) throw new UnauthorizedResponse("Token is missing.");

            final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
            if(!JwtManager.verifyToken(token, hashedRealKey, plugin.getConfig().salt)) {
                throw new UnauthorizedResponse("Token is invalid.");
            }
        });

        // Controllers
        AuthController authController = new AuthController(plugin);
        BannedIpsController bannedIpsController = new BannedIpsController(plugin);
        ControlController controlController = new ControlController(plugin);
        GamerulesController gamerulesController = new GamerulesController(plugin);
        InfoController infoController = new InfoController(plugin);
        LogsController logsController = new LogsController(plugin);
        MonitorController monitorController = new MonitorController(plugin);
        PlayersController playersController = new PlayersController(plugin);
        SecurityController securityController = new SecurityController(plugin);
        VersionController versionController = new VersionController(plugin);
        WhitelistController whitelistController = new WhitelistController(plugin);

        // API Routes
        app.routes(() -> path("api", () -> {
            get("auth", authController.getCram);
            post("auth", authController.validateCram);
            get("monitor", monitorController.getMonitor);
            path("banned-ips", () -> {
                get("/", bannedIpsController.getBannedIps);
                post("add", bannedIpsController.banIp);
                post("remove", bannedIpsController.pardonIp);
            });
            path("players", () -> {
                get("/", playersController.getPlayers);
                post("op", playersController.giveOp);
                post("deop", playersController.takeOp);
                post("kick", playersController.kickPlayer);
                post("ban", playersController.banPlayer);
                post("pardon", playersController.pardonPlayer);
                post("gamemode", playersController.setGamemode);
                delete("/", playersController.deletePlayerData);
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
            path("info", () -> {
                get("/", infoController.getServerInfo);
                post("motd", infoController.setMotd);
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
            path("logs", () -> {
                get("/", logsController.getLogFileList);
                get("{fileName}", logsController.getLogContent);
                delete("/", logsController.clearLogs);
                delete("{fileName}", logsController.deleteLog);
            });
        }));

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
            plugin.logger.info("Web server is stopped.");
        }
    }

    public boolean isRunning() {
        JettyServer jettyServer = app.jettyServer();
        return app != null && jettyServer != null && jettyServer.started;
    }

    public String getJettyVersion() {
        return Jetty.VERSION;
    }
}
