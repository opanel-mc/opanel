package net.opanel.web;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import io.javalin.http.HttpStatus;
import io.javalin.jetty.JettyServer;
import io.javalin.json.JavalinGson;
import io.javalin.util.JavalinLogger;
import net.opanel.OPanel;
import net.opanel.common.ServerType;
import net.opanel.config.OPanelConfiguration;
import net.opanel.controller.BaseController;
import net.opanel.controller.BeforeController;
import net.opanel.controller.ErrorController;
import net.opanel.controller.ExtensionPageController;
import net.opanel.controller.api.*;
import net.opanel.controller.openapi.*;
import net.opanel.endpoint.*;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.URLResourceFactory;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebServer {
    public static final String ROOT_PATH = "web";
    public final String HOST;
    public final int PORT;

    private final OPanel plugin;
    private final Set<BaseEndpoint> endpoints = ConcurrentHashMap.newKeySet();
    private Javalin app;
    private boolean isResourceFactoryRegistered = false;

    public WebServer(OPanel plugin) {
        this.plugin = plugin;
        HOST = getConfiguredHost(plugin);
        PORT = plugin.getConfig().webServerPort;

        JavalinLogger.enabled = false;
    }

    private static String getConfiguredHost(OPanel plugin) {
        String host = plugin.getConfig().webServerHost;
        if(host == null || host.isBlank()) {
            return OPanelConfiguration.defaultConfig.webServerHost;
        }
        return host;
    }

    private void registerEndpoint(BaseEndpoint endpoint) {
        endpoints.add(endpoint);
    }

    private void buildRoutes() {
        // Websocket
        ws("/socket/players", ws -> registerEndpoint(new PlayersEndpoint(ws, plugin)));
        ws("/socket/inventory/{uuid}", ws -> registerEndpoint(new InventoryEndpoint(ws, plugin)));
        ws("/socket/terminal", ws -> registerEndpoint(new TerminalEndpoint(ws, plugin)));
        ws("/socket/map", ws -> registerEndpoint(new MapEndpoint(ws, plugin)));
        ws("/socket/monitor", ws -> registerEndpoint(new MonitorEndpoint(ws, plugin)));

        // API Controllers
        BeforeController beforeController = new BeforeController(plugin);
        AssetsController assetsController = new AssetsController(plugin);
        DownloadController downloadController = new DownloadController(plugin);
        AuthController authController = new AuthController(plugin);
        OidcController oidcController = new OidcController(plugin);
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
        ExtensionsController extensionsController = new ExtensionsController(plugin);
        ExtensionPageController extensionPageController = new ExtensionPageController(plugin);

        // API Routes
        before("/*", beforeController.beforeAll);
        before("/*", beforeController.handleRsc);
        before("/*", beforeController.handleFonts);
        get("/panel/ext", ctx -> ctx.status(HttpStatus.NOT_FOUND));
        get("/panel/ext/", ctx -> ctx.status(HttpStatus.NOT_FOUND));
        get("/panel/ext/{extId}", extensionPageController.getExtensionPage);
        get("/panel/ext/{extId}/", extensionPageController.getExtensionPage);
        get("/panel/ext/{extId}/<resource>", extensionPageController.getExtensionPage);
        path("assets", () -> {
            before("/upload/*", beforeController.authToken);
            get("/{name}", assetsController.getAsset);
            post("/upload/{name}", assetsController.uploadAsset);
            delete("/reset/{name}", assetsController.resetAsset);
        });
        path("file", () -> {
            before("/*", beforeController.authToken);
            get("/{id}/{fileName}", downloadController.downloadFile);
        });
        path("api", () -> {
            before("/*", beforeController.authToken);

            path("auth", () -> {
                get("/", authController.getCram);
                post("/", authController.validateCram);
                post("/check", authController.checkAuth);
                post("/logout", authController.logout);
                path("oidc", () -> {
                    get("login", oidcController.login);
                    get("callback", oidcController.callback);
                    post("bind-user", oidcController.bindNewUser);
                    get("config", oidcController.getConfig);
                    get("allowed-users", oidcController.getAllowedUsers);
                    post("allowed-users", oidcController.addAllowedUser);
                    delete("allowed-users", oidcController.removeAllowedUser);
                });
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
                get("paper-config", controlController.getPaperServerConfig);
                post("paper-config", controlController.setPaperServerConfig);
                get("paper-world-config", controlController.getPaperWorldConfig);
                post("paper-world-config", controlController.setPaperWorldConfig);
                get("launch-command", controlController.getLaunchCommand);
                post("launch-command", controlController.setLaunchCommand);
            });
            path("gamerules", () -> {
                get("{dimName}", gamerulesController.getGamerules);
                post("{dimName}", gamerulesController.changeGamerule);
                patch("{dimName}", gamerulesController.patchGamerule); // for mcp
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
                post("{fileName}/upload-mclogs", logsController.uploadLogToMclogs);
            });
            path("map", () -> {
                get("/", mapController.getMapEnabled);
                post("/", mapController.toggleMap);
                get("{saveName}", mapController.getAvailableTiles);
                post("{saveName}/tiles-range", mapController.getTilesInRange);
                post("{saveName}/tiles", mapController.getTiles);
            });
            path("monitor", () -> {
                get("/", monitorController.getMonitorSnapshot); // for mcp
                get("activity", monitorController.getActivity);
            });
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
            path("extensions", () -> {
                get("/", extensionsController.getExtensions);
                post("/", extensionsController.uploadExtension);
                get("{fileName}", extensionsController.downloadExtension);
                post("{fileName}", extensionsController.toggleExtension);
                delete("{fileName}", extensionsController.deleteExtension);
            });
            path("extension-res", () -> {
                get("/", extensionsController.getRegisteredExtensionPages);
                get("{extId}", extensionsController.getExtensionResource);
                get("{extId}/", extensionsController.getExtensionResource);
                get("{extId}/<resource>", extensionsController.getExtensionResource);
            });
            before("extension/{extId}/<path>", beforeController.routeExtensionBackend);
        });

        // Open API Controllers
        OpenInfoController openInfoController = new OpenInfoController(plugin);
        OpenMonitorController openMonitorController = new OpenMonitorController(plugin);
        OpenPluginsController openPluginsController = new OpenPluginsController(plugin);
        OpenPlayersController openPlayersController = new OpenPlayersController(plugin);
        OpenLogsController openLogsController = new OpenLogsController(plugin);

        // Open API Routes
        path("open-api", () -> {
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
            path("logs", () -> {
                get("/", openLogsController.getLogFileList);
                get("{fileName}", openLogsController.getLogContent);
                get("{fileName}/download", openLogsController.downloadLog);
            });
        });
    }

    public void start() throws Exception {
        if(
            !isResourceFactoryRegistered && (
                plugin.getServer().getServerType() == ServerType.FORGE
                || plugin.getServer().getServerType() == ServerType.NEOFORGE
            )
        ) {
            ResourceFactory.registerResourceFactory("union", new URLResourceFactory());
            isResourceFactoryRegistered = true;
        }

        app = Javalin.create(config -> {
            config.startup.showJavalinBanner = false;

            // Gson configuration
            config.jsonMapper(new JavalinGson(new Gson(), false));

            // CORS configuration
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.path = "/open-api/*";
                    it.anyHost();
                });
                cors.addRule(it -> {
                    it.path = "/api/*";
                    it.allowHost("http://localhost:3001"); // for dev
                    it.allowCredentials = true;
                });
                cors.addRule(it -> {
                    it.path = "/assets/*";
                    it.allowHost("http://localhost:3001"); // for dev
                    it.allowCredentials = true;
                });
                cors.addRule(it -> {
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
                staticFiles.skipFileFunction = request -> (
                    request.getRequestURI().equals("/panel/ext")
                    || request.getRequestURI().startsWith("/panel/ext/")
                );
                staticFiles.mimeTypes.add("text/x-component", "rsc");
            });

            // Routes
            config.routes.apiBuilder(this::buildRoutes);

            // Not found page
            ErrorController errorController = new ErrorController(plugin);
            config.routes.error(HttpStatus.NOT_FOUND, errorController.notFound);

            // Exception handling
            config.routes.exception(Exception.class, (e, ctx) -> {
                e.printStackTrace();
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);

                HashMap<String, Object> jsonObj = new HashMap<>();
                jsonObj.put("code", 500);
                jsonObj.put("error", e.getMessage());
                ctx.json(jsonObj);
            });

            config.events.serverStopping(() -> {
                endpoints.forEach(BaseEndpoint::shutdown);
                endpoints.clear();
                BaseController.unregisterAllControllerInstances();
            });
        });

        app.start(HOST, PORT);
        plugin.logger.info("OPanel web server is ready on "+ HOST +":"+ PORT);
        plugin.initializeAccessKey();
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
        return jettyServer != null && jettyServer.started();
    }
}
