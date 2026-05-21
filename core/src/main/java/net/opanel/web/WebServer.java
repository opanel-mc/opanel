package net.opanel.web;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import io.javalin.http.HttpStatus;
import io.javalin.jetty.JettyServer;
import io.javalin.json.JavalinGson;
import io.javalin.util.JavalinLogger;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.controller.BeforeController;
import net.opanel.controller.ErrorController;
import net.opanel.controller.api.*;
import net.opanel.controller.openapi.OpenInfoController;
import net.opanel.controller.openapi.OpenMonitorController;
import net.opanel.controller.openapi.OpenPlayersController;
import net.opanel.controller.openapi.OpenPluginsController;
import net.opanel.endpoint.InventoryEndpoint;
import net.opanel.endpoint.MapEndpoint;
import net.opanel.endpoint.PlayersEndpoint;
import net.opanel.endpoint.TerminalEndpoint;

import java.util.HashMap;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebServer {
    public static final String ROOT_PATH = "web";
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

            // HTTP response compression
            config.compression.gzipOnly(6);

            // Gson configuration
            config.jsonMapper(new JavalinGson(new Gson()));

            // CORS configuration
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.path = "/open-api/*";
                    it.anyHost();
                });
                cors.add(it -> {
                    it.path = "/api/*";
                    it.allowHost("http://localhost:3001"); // for dev
                    it.allowCredentials = true;
                });
                cors.add(it -> {
                    it.path = "/assets/*";
                    it.allowHost("http://localhost:3001"); // for dev
                    it.allowCredentials = true;
                });
                cors.add(it -> {
                    it.path = "/file/*";
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
                staticFiles.directory = "/"+ ROOT_PATH;
            });
        });

        // Websocket
        app.ws("/socket/players", ws -> new PlayersEndpoint(app, ws, plugin));
        app.ws("/socket/inventory/{uuid}", ws -> new InventoryEndpoint(app, ws, plugin));
        app.ws("/socket/terminal", ws -> new TerminalEndpoint(app, ws, plugin));
        app.ws("/socket/map", ws -> new MapEndpoint(app, ws, plugin));

        // API Controllers
        BeforeController beforeController = new BeforeController(plugin);
        ErrorController errorController = new ErrorController(plugin);
        AssetsController assetsController = new AssetsController(plugin);
        DownloadController downloadController = new DownloadController(plugin);
        AuthController authController = new AuthController(plugin);
        BannedIpsController bannedIpsController = new BannedIpsController(plugin);
        ControlController controlController = new ControlController(plugin);
        GamerulesController gamerulesController = new GamerulesController(plugin);
        IconController iconController = new IconController(plugin);
        InfoController infoController = new InfoController(plugin);
        LogsController logsController = new LogsController(plugin);
        MapController mapController = new MapController(plugin);
        MonitorController monitorController = new MonitorController(plugin);
        PlayersController playersController = new PlayersController(plugin);
        SavesController savesController = new SavesController(plugin);
        PluginsController pluginsController = new PluginsController(plugin);
        TerminalController terminalController = new TerminalController(plugin);
        SecurityController securityController = new SecurityController(plugin);
        VersionController versionController = new VersionController(plugin);
        WhitelistController whitelistController = new WhitelistController(plugin);
        TasksController tasksController = new TasksController(plugin);
        McpController mcpController = new McpController(plugin);
        OpenAPIController openAPIController = new OpenAPIController(plugin);

        // API Routes
        app.before("/*", beforeController.beforeAll);
        app.before("/*", beforeController.handleRsc);
        app.before("/*", beforeController.handleFonts);
        app.routes(() -> path("assets", () -> {
            before("/upload/*", beforeController.authToken);
            get("/{name}", assetsController.getAsset);
            post("/upload/{name}", assetsController.uploadAsset);
            delete("/reset/{name}", assetsController.resetAsset);
        }));
        app.routes(() -> path("file", () -> {
            before("/*", beforeController.authToken);
            get("/{id}/{fileName}", downloadController.downloadFile);
        }));
        app.routes(() -> path("api", () -> {
            before("/*", beforeController.authToken);

            path("auth", () -> {
                get("/", authController.getCram);
                post("/", authController.validateCram);
                post("/check", authController.checkAuth);
                post("/logout", authController.logout);
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
                post("restart", controlController.restartServer);
                post("world", controlController.switchSave);
                get("bukkit-config", controlController.getBukkitServerConfig);
                post("bukkit-config", controlController.setBukkitServerConfig);
                get("paper-world-config", controlController.getPaperWorldConfig);
                post("paper-world-config", controlController.setPaperWorldConfig);
                get("launch-command", controlController.getLaunchCommand);
                post("launch-command", controlController.setLaunchCommand);
            });
            path("gamerules", () -> {
                get("/", gamerulesController.getGamerules);
                post("/", gamerulesController.changeGamerule);
                patch("/", gamerulesController.patchGamerule); // for mcp
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
            path("map", () -> {
                get("/", mapController.getMapEnabled);
                post("/", mapController.toggleMap);
                get("{saveName}", mapController.getAvailableTiles);
                post("{saveName}/tiles-range", mapController.getTilesInRange);
                post("{saveName}/tiles", mapController.getTiles);
            });
            get("monitor", monitorController.getMonitor);
            path("players", () -> {
                get("/", playersController.getPlayersOverview);
                get("list", playersController.getPlayers); // for mcp
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
                patch("{saveName}", savesController.toggleSaveDatapack);
                delete("{saveName}", savesController.deleteSave);
            });
            path("plugins", () -> {
                get("/", pluginsController.getPlugins);
                get("/icon/{fileName}", pluginsController.getPluginIcon);
                post("/", pluginsController.uploadPlugin);
                get("{fileName}", pluginsController.downloadPlugin);
                post("{fileName}", pluginsController.togglePlugin);
                delete("{fileName}", pluginsController.deletePlugin);
            });
            path("terminal", () -> {
                get("/", terminalController.getCommands); // for mcp
                post("/", terminalController.sendCommand); // for mcp
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
            path("tasks", () -> {
                get("/", tasksController.getTasks);
                post("/", tasksController.createTask);
                post("/{id}", tasksController.editTask);
                patch("/{id}", tasksController.toggleTask);
                delete("/{id}", tasksController.deleteTask);
            });
            path("mcp", () -> {
                get("/", mcpController.getMcpEnabled);
                post("/", mcpController.toggleMcp);
                get("/token", mcpController.getMaskedAccessToken);
                post("/token", mcpController.generateAccessToken);
            });
            path("open-api", () -> {
                get("/", openAPIController.getOpenAPIEnabled);
                post("/", openAPIController.toggleOpenAPI);
                get("/{interfaceName}", openAPIController.getInterfaceEnabled);
                post("/{interfaceName}", openAPIController.toggleInterface);
            });
        }));

        // Open API Controllers
        OpenInfoController openInfoController = new OpenInfoController(plugin);
        OpenMonitorController openMonitorController = new OpenMonitorController(plugin);
        OpenPluginsController openPluginsController = new OpenPluginsController(plugin);
        OpenPlayersController openPlayersController = new OpenPlayersController(plugin);

        // Open API Routes
        app.routes(() -> path("open-api", () -> {
            before("/*", beforeController.handleOpenAPI);

            get("info", openInfoController.getServerInfo);
            get("monitor", openMonitorController.getMonitor);
            path("plugins", () -> {
                get("/", openPluginsController.getPlugins);
                get("/icon/{fileName}", openPluginsController.getPluginIcon);
            });
            path("players", () -> {
                get("/", openPlayersController.getPlayers);
                get("/{uuid}", openPlayersController.getPlayerInfo);
            });
        }));

        // Not found page
        app.error(HttpStatus.NOT_FOUND, errorController.notFound);

        // Exception handling
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);

            HashMap<String, Object> jsonObj = new HashMap<>();
            jsonObj.put("code", 500);
            jsonObj.put("error", e.getMessage());
            ctx.json(jsonObj);
        });

        app.start(PORT);
        plugin.logger.info("OPanel web server is ready on port "+ PORT);
        plugin.initializeAccessKey();

        app.events(event -> {
            event.serverStopping(BaseController::unregisterAllControllerInstances);
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
