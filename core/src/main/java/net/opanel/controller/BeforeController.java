package net.opanel.controller;

import io.javalin.http.*;
import io.javalin.http.servlet.JavalinServletContext;
import net.opanel.OPanel;
import net.opanel.config.McpConfiguration;
import net.opanel.config.OpenAPIConfiguration;
import net.opanel.extension.ExtensionManager;
import net.opanel.extension.LoadedExtension;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;
import net.opanel.web.JwtManager;

public class BeforeController extends BaseController {
    private static final String DEFAULT_RSC_FILE = "index.txt";

    public BeforeController(OPanel plugin) {
        super(plugin);
    }

    public Handler beforeAll = ctx -> {
        ctx.header("X-Powered-By", "OPanel");
    };

    public Handler authToken = ctx -> {
        if(ctx.path().startsWith("/api/auth") || ctx.path().equals("/api/icon") || ctx.method() == HandlerType.OPTIONS) return;

        String authorization = ctx.header("Authorization");
        if(authorization != null && authorization.startsWith("Bearer ") && !ctx.path().startsWith("/api/security")) { // auth mcp access token
            String accessToken = authorization.substring(7);
            if(!accessToken.startsWith("o-") || accessToken.length() != 50) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Authorization header is invalid.");
                clearContextTasks(ctx);
                return;
            }

            McpConfiguration mcpConfig = Storage.get().getStoredData(StorageKey.MCP_CONFIG);
            if(mcpConfig == null || !mcpConfig.enabled) {
                sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Mcp is not enabled.");
                clearContextTasks(ctx);
                return;
            }
            if(!accessToken.equals(mcpConfig.accessToken)) {
                sendResponse(ctx, HttpStatus.UNAUTHORIZED, "Mcp access token is invalid.");
                clearContextTasks(ctx);
            }
            return;
        }

        String token = ctx.cookie("token"); // jws
        if(token == null) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED, "Token is missing.");
            clearContextTasks(ctx);
            return;
        }

        final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
        if(!JwtManager.verifyToken(token, hashedRealKey, plugin.getConfig().salt)) {
            ctx.removeCookie("token");
            sendResponse(ctx, HttpStatus.UNAUTHORIZED, "Token is invalid.");
            clearContextTasks(ctx);
        }
    };

    /** @see <a href="https://github.com/vercel/next.js/discussions/59394">https://github.com/vercel/next.js/discussions/59394</a> */
    public Handler handleRsc = ctx -> {
        String reqPath = ctx.path();
        if(!reqPath.contains(".txt") || reqPath.contains(DEFAULT_RSC_FILE)) return;

        // Request robots.txt file
        if(reqPath.equals("/robots.txt") || reqPath.equals("/llms.txt")) return;

        // Maybe a next.js bug, which will lead user to <page_name>.txt file without _rsc param
        // just redirect it to the correct page
        if(ctx.queryParam("_rsc") == null) {
            ctx.redirect(reqPath.replaceAll("\\.txt/?$", "") +"?"+ ctx.queryString());
            return;
        }

        // Rsc file request
        ctx.redirect(reqPath.replaceAll("\\.txt$", "/"+ DEFAULT_RSC_FILE) +"?"+ ctx.queryString());
    };

    public Handler handleFonts = ctx -> {
        if(ctx.path().endsWith(".ttf")) {
            ctx.status(HttpStatus.OK);
            ctx.contentType(ContentType.FONT_TTF);
        }
        if(ctx.path().endsWith(".otf")) {
            ctx.status(HttpStatus.OK);
            ctx.contentType(ContentType.FONT_OTF);
        }
    };

    public Handler handleOpenAPI = ctx -> {
        OpenAPIConfiguration openAPIConfig = Storage.get().getStoredData(StorageKey.OPEN_API_CONFIG);
        if(openAPIConfig == null || !openAPIConfig.enabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Open API is not enabled.");
            clearContextTasks(ctx);
            return;
        }

        String interfaceName = getOpenAPIInterfaceName(ctx.path());
        if(interfaceName == null || !OpenAPIConfiguration.isValidInterfaceName(interfaceName)) {
            return;
        }

        if(openAPIConfig.interfaces == null) return;

        Boolean interfaceEnabled = openAPIConfig.interfaces.get(interfaceName);
        if(interfaceEnabled != null && !interfaceEnabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Interface '"+ interfaceName +"' is not enabled.");
            clearContextTasks(ctx);
        }
    };

    public Handler routeExtensionBackend = ctx -> {
        String extensionId = ctx.pathParam("extId");
        String path = ctx.pathParamMap().containsKey("path") ? ctx.pathParam("path") : "";
        String normalizedPath = Utils.normalizePath(path);
        if(normalizedPath == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid extension backend path.");
            clearContextTasks(ctx);
            return;
        }

        ExtensionManager extensionManager = plugin.getExtensionManager();
        if(!extensionManager.hasExtension(extensionId)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension not found.");
            clearContextTasks(ctx);
            return;
        }

        LoadedExtension extension = extensionManager.getExtension(extensionId);
        LoadedExtension.BackendRoute route = extension.getBackendRoute(normalizedPath);
        if(route == null || route.method != ctx.method()) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension backend path not found.");
            clearContextTasks(ctx);
            return;
        }

        route.handler.handle(ctx);
        clearContextTasks(ctx);
    };

    private String getOpenAPIInterfaceName(String path) {
        final String prefix = "/open-api/";
        if(path == null || !path.startsWith(prefix)) return null;

        String routePath = path.substring(prefix.length());
        if(routePath.isEmpty()) return null;

        int splitIndex = routePath.indexOf('/');
        if(splitIndex == -1) return routePath;

        return routePath.substring(0, splitIndex);
    }

    private void clearContextTasks(Context ctx) {
        ((JavalinServletContext) ctx).getTasks().clear();
    }
}
