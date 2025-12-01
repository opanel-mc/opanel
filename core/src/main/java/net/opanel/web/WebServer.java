package net.opanel.web;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.jetty.JettyServer;
import io.javalin.json.JavalinGson;
import io.javalin.util.JavalinLogger;
import net.opanel.OPanel;
import net.opanel.api.*;
import net.opanel.terminal.TerminalEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebServer {
    public final int PORT;

    private final OPanel plugin;
    private Javalin app;
    // 缓存 HTML 页面内容
    private final Map<String, String> pageCache = new HashMap<>();

    public WebServer(OPanel plugin) {
        this.plugin = plugin;
        PORT = plugin.getConfig().webServerPort;

        JavalinLogger.enabled = false;

        // 预加载页面到缓存
        preloadPages();
    }

    /**
     * 预加载关键页面到内存缓存
     */
    private void preloadPages() {
        String[] pagesToPreload = {
                "/", "/about", "/login", "/panel", "/404",
                "/panel/code-of-conduct", "/panel/dashboard", "/panel/gamerules",
                "/panel/logs", "/panel/logs/view", "/panel/players", "/panel/plugins",
                "/panel/saves", "/panel/settings", "/panel/terminal"
        };

        for (String page : pagesToPreload) {
            loadPageToCache(page);
        }
    }

    /**
     * 加载页面到缓存
     */
    private void loadPageToCache(String route) {
        try {
            String resourcePath = getResourcePath(route);
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);

            if (inputStream != null) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                pageCache.put(route, content);
            } else {
                plugin.logger.warn("Failed to load the page resources: " + resourcePath);
            }
        } catch (Exception e) {
            plugin.logger.warn("Failed to preload the page: " + route + " - " + e.getMessage());
        }
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
                });
            });

            // Multipart configuration
            config.jetty.multipartConfig.cacheDirectory(OPanel.TMP_DIR_PATH.toString());
            config.jetty.multipartConfig.maxInMemoryFileSize(10, SizeUnit.MB);

            // Frontend - 使用类路径资源
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/web"; // 类路径中的 /web 目录
                staticFiles.location = Location.CLASSPATH;
                staticFiles.precompress = false;
            });
        });

        // Websocket
        app.ws("/terminal", ws -> new TerminalEndpoint(ws, plugin));

        // Controllers
        BeforeController beforeController = new BeforeController(plugin);
        ErrorController errorController = new ErrorController(plugin);
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

        setupNextJsRoutes(app);

        // Not found page
        app.error(HttpStatus.NOT_FOUND, errorController.notFound);

        app.start(PORT);
        plugin.logger.info("OPanel web server is ready on port "+ PORT);
        plugin.logger.info("Using embedded web resources from classpath");
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

    private void setupNextJsRoutes(Javalin app) {
        String[] nextJsRoutes = {
                "/about",
                "/login",
                "/panel",
                "/panel/code-of-conduct",
                "/panel/dashboard",
                "/panel/gamerules",
                "/panel/logs",
                "/panel/logs/view",
                "/panel/players",
                "/panel/plugins",
                "/panel/saves",
                "/panel/settings",
                "/panel/terminal"
        };

        for (String route : nextJsRoutes) {
            app.get(route, ctx -> {
                serveNextJsPage(ctx, route);
            });
        }

        // 处理根路径
        app.get("/", ctx -> {
            serveNextJsPage(ctx, "/");
        });
    }

    private void serveNextJsPage(io.javalin.http.Context ctx, String route) {
        try {
            // 首先尝试从缓存获取
            String cachedContent = pageCache.get(route);
            if (cachedContent != null) {
                ctx.contentType("text/html");
                ctx.result(cachedContent);
                return;
            }

            // 缓存未命中，从类路径加载
            String resourcePath = getResourcePath(route);
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);

            if (inputStream != null) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                ctx.contentType("text/html");
                ctx.result(content);

                // 添加到缓存
                pageCache.put(route, content);
            } else {
                // 尝试返回 404 页面
                String notFoundContent = pageCache.get("/404");
                if (notFoundContent != null) {
                    ctx.contentType("text/html");
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.result(notFoundContent);
                } else {
                    ctx.status(404).result("Page not found: " + route);
                }
            }
        } catch (Exception e) {
            plugin.logger.error("Error serving page: " + route + " - " + e.getMessage());
            ctx.status(500).result("Error serving page: " + e.getMessage());
        }
    }

    /**
     * 根据路由获取类路径资源路径
     */
    private String getResourcePath(String route) {
        if ("/".equals(route)) {
            return "/web/index.html";
        }

        String path = route.startsWith("/") ? route.substring(1) : route;
        return "/web/" + path + "/index.html";
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
}