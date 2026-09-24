package net.opanel.web;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import io.javalin.http.HandlerType;
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
import static net.opanel.web.AuthRouteRole.*;

public class WebServer {
    public static final String ROOT_PATH = "web";
    public final String HOST;
    public final int PORT;

    private final OPanel plugin;
    private final Set<BaseEndpoint> endpoints = ConcurrentHashMap.newKeySet();
    private Javalin app;
    private boolean isResourceFactoryRegistered = false;
    private boolean initialAccessKeyNoticePending = false;

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
        beforeMatched("/*", beforeController.authToken);
        get("/panel/ext", ctx -> ctx.status(HttpStatus.NOT_FOUND));
        get("/panel/ext/", ctx -> ctx.status(HttpStatus.NOT_FOUND));
        get("/panel/ext/{extId}", extensionPageController.getExtensionPage);
        get("/panel/ext/{extId}/", extensionPageController.getExtensionPage);
        get("/panel/ext/{extId}/<resource>", extensionPageController.getExtensionPage);
        path("assets", () -> {
            get("/{name}", assetsController.getAsset, PUBLIC);
            post("/upload/{name}", assetsController.uploadAsset, PANEL_OR_MCP);
            delete("/reset/{name}", assetsController.resetAsset, PANEL_OR_MCP);
        });
        path("file", Set.of(PANEL_OR_MCP), () -> {
            get("/{id}/{fileName}", downloadController.downloadFile);
        });
        path("api", () -> {
            path("auth", () -> {
                get("/", authController.getCram, PUBLIC);
                post("/", authController.validateCram, PUBLIC);
                post("/check", authController.checkAuth, PUBLIC);
                post("/logout", authController.logout, PUBLIC);
                path("oidc", () -> {
                    get("login", oidcController.login, PUBLIC);
                    get("callback", oidcController.callback, PUBLIC);
                    post("bind-user", oidcController.bindNewUser, PUBLIC);
                    get("config", oidcController.getConfig, PUBLIC);
                    get("allowed-users", oidcController.getAllowedUsers, PANEL_SESSION);
                    post("allowed-users", oidcController.addAllowedUser, PANEL_SESSION);
                    delete("allowed-users", oidcController.removeAllowedUser, PANEL_SESSION);
                });
            });
            path("banned-ips", Set.of(PANEL_OR_MCP), () -> {
                get("/", bannedIpsController.getBannedIps);
                post("add", bannedIpsController.banIp);
                post("remove", bannedIpsController.pardonIp);
            });
            path("control", Set.of(PANEL_OR_MCP), () -> {
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
            path("gamerules", Set.of(PANEL_OR_MCP), () -> {
                get("{dimName}", gamerulesController.getGamerules);
                post("{dimName}", gamerulesController.changeGamerule);
                patch("{dimName}", gamerulesController.patchGamerule); // for mcp
            });
            path("icon", () -> {
                get("/", iconController.getFavicon, PUBLIC);
                post("/", iconController.uploadFavicon, PANEL_OR_MCP);
            });
            path("info", Set.of(PANEL_OR_MCP), () -> {
                get("/", infoController.getServerInfo);
                post("motd", infoController.setMotd);
            });
            path("logs", Set.of(PANEL_OR_MCP), () -> {
                get("/", logsController.getLogFileList);
                get("{fileName}", logsController.getLogContent);
                get("{fileName}/download", logsController.downloadLog);
                delete("/", logsController.clearLogs);
                delete("{fileName}", logsController.deleteLog);
                post("{fileName}/upload-mclogs", logsController.uploadLogToMclogs);
            });
            path("map", Set.of(PANEL_OR_MCP), () -> {
                get("/", mapController.getMapEnabled);
                post("/", mapController.toggleMap);
                get("{saveName}", mapController.getAvailableTiles);
                post("{saveName}/tiles-range", mapController.getTilesInRange);
                post("{saveName}/tiles", mapController.getTiles);
            });
            path("monitor", Set.of(PANEL_OR_MCP), () -> {
                get("/", monitorController.getMonitorSnapshot); // for mcp
                get("history", monitorController.getHistory);
                get("activity", monitorController.getActivity);
            });
            path("players", Set.of(PANEL_OR_MCP), () -> {
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
            path("saves", Set.of(PANEL_OR_MCP), () -> {
                get("/", savesController.getSaves);
                post("/", savesController.uploadSave);
                get("{saveName}", savesController.downloadSave);
                post("{saveName}", savesController.editSave);
                patch("{saveName}", savesController.toggleSaveDatapack);
                delete("{saveName}", savesController.deleteSave);
            });
            path("plugins", Set.of(PANEL_OR_MCP), () -> {
                get("/", pluginsController.getPlugins);
                get("/icon/{fileName}", pluginsController.getPluginIcon);
                post("/", pluginsController.uploadPlugin);
                get("{fileName}", pluginsController.downloadPlugin);
                post("{fileName}", pluginsController.togglePlugin);
                delete("{fileName}", pluginsController.deletePlugin);
            });
            path("terminal", Set.of(PANEL_OR_MCP), () -> {
                get("/", terminalController.getCommands); // for mcp
                post("/", terminalController.sendCommand); // for mcp
            });
            post("security", securityController.updateAccessKey, PANEL_SESSION);
            get("version", versionController.getVersionInfo, PANEL_OR_MCP);
            path("whitelist", Set.of(PANEL_OR_MCP), () -> {
                get("/", whitelistController.getWhitelist);
                post("enable", whitelistController.enableWhitelist);
                post("disable", whitelistController.disableWhitelist);
                post("write", whitelistController.writeWhitelist);
                post("add", whitelistController.addWhitelistEntry);
                post("remove", whitelistController.removeWhitelistEntry);
            });
            path("tasks", Set.of(PANEL_OR_MCP), () -> {
                get("/", tasksController.getTasks);
                post("/", tasksController.createTask);
                post("/{id}", tasksController.editTask);
                patch("/{id}", tasksController.toggleTask);
                delete("/{id}", tasksController.deleteTask);
            });
            path("mcp", () -> {
                get("/", mcpController.getMcpEnabled, PANEL_OR_MCP);
                post("/", mcpController.toggleMcp, PANEL_OR_MCP);
                get("/token", mcpController.getMaskedAccessToken, PANEL_SESSION);
                post("/token", mcpController.generateAccessToken, PANEL_SESSION);
            });
            path("open-api", Set.of(PANEL_OR_MCP), () -> {
                get("/", openAPIController.getOpenAPIEnabled);
                post("/", openAPIController.toggleOpenAPI);
                get("/{interfaceName}", openAPIController.getInterfaceEnabled);
                post("/{interfaceName}", openAPIController.toggleInterface);
            });
            path("extensions", Set.of(PANEL_OR_MCP), () -> {
                get("/", extensionsController.getExtensions);
                post("/", extensionsController.uploadExtension);
                get("{fileName}", extensionsController.downloadExtension);
                post("{fileName}", extensionsController.toggleExtension);
                delete("{fileName}", extensionsController.deleteExtension);
            });
            path("extension-res", Set.of(PANEL_OR_MCP), () -> {
                get("/", extensionsController.getRegisteredExtensionPages);
                get("{extId}", extensionsController.getExtensionResource);
                get("{extId}/", extensionsController.getExtensionResource);
                get("{extId}/<resource>", extensionsController.getExtensionResource);
            });
            String extensionBackendPath = prefixPath("extension/{extId}/<path>");
            HandlerType.values().stream()
                    .filter(HandlerType::isHttpMethod)
                    .forEach(method -> staticInstance().addHttpHandler(
                            method,
                            extensionBackendPath,
                            beforeController.routeExtensionBackend,
                            PANEL_OR_MCP
                    ));
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
        initialAccessKeyNoticePending |= plugin.initializeAccessKey();

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
        if(initialAccessKeyNoticePending) {
            plugin.logger.warn("===========================OPanel===========================");
            plugin.logger.warn("Initial launching detected,");
            plugin.logger.warn("Check opanel/INITIAL_ACCESS_KEY.txt for the initial access key.");
            plugin.logger.warn("Remember to delete the file for your server security.");
            plugin.logger.warn("============================================================");
            initialAccessKeyNoticePending = false;
        }
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
