package net.opanel.controller;

import io.javalin.http.*;
import io.javalin.security.RouteRole;
import net.opanel.OPanel;
import net.opanel.config.McpConfiguration;
import net.opanel.config.OpenAPIConfiguration;
import net.opanel.extension.ExtensionManager;
import net.opanel.extension.LoadedExtension;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;
import net.opanel.web.AuthRouteRole;
import net.opanel.web.JwtManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

public class BeforeController extends BaseController {
    private static final String RSC_COMPATIBILITY_ID_RESOURCE = "vinext-rsc-compatibility-id";
    private String rscCompatibilityId;

    public BeforeController(OPanel plugin) {
        super(plugin);

        rscCompatibilityId = loadRscCompatibilityId();
        if(rscCompatibilityId == null) {
            plugin.logger.warn("Cannot find vinext RSC compatibility ID. Client-side page navigation may fall back to full page reloads.");
        }
    }

    /**
     * Loads the build-specific vinext RSC compatibility ID written to Java resources by the frontend bundler.
     * It must match the ID embedded in the client bundle, otherwise vinext falls back to a full page reload.
     */
    private String loadRscCompatibilityId() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(RSC_COMPATIBILITY_ID_RESOURCE)) {
            if(is == null) return null;

            String compatibilityId = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            return compatibilityId.isEmpty() ? null : compatibilityId;
        } catch(IOException e) {
            plugin.logger.warn("Failed to read vinext RSC compatibility ID: "+ e.getMessage());
            return null;
        }
    }

    public Handler beforeAll = ctx -> {
        ctx.header("X-Powered-By", "OPanel");
        ctx.header("x-nextjs-deployment-id", rscCompatibilityId);
    };

    public Handler authToken = ctx -> {
        if(ctx.method().equals(HandlerType.OPTIONS) || !isManagedAuthPath(ctx.path())) return;

        Set<RouteRole> roles = ctx.routeRoles();
        if(roles.size() != 1 || !(roles.iterator().next() instanceof AuthRouteRole role)) {
            plugin.logger.error("Route authorization is not configured for "+ ctx.method() +" "+ ctx.path());
            reject(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Route authorization is not configured.");
            return;
        }

        if(role == AuthRouteRole.PUBLIC) return;

        String authorization = ctx.header("Authorization");
        if(role == AuthRouteRole.PANEL_OR_MCP && authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring(7);
            if(!accessToken.startsWith("o-") || accessToken.length() != 50) {
                reject(ctx, HttpStatus.BAD_REQUEST, "Authorization header is invalid.");
                return;
            }

            McpConfiguration mcpConfig = Storage.get().getStoredData(StorageKey.MCP_CONFIG);
            if(mcpConfig == null || !mcpConfig.enabled) {
                reject(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Mcp is not enabled.");
                return;
            }
            if(!constantTimeEquals(accessToken, mcpConfig.accessToken)) {
                reject(ctx, HttpStatus.UNAUTHORIZED, "Mcp access token is invalid.");
            }
            return;
        }

        String token = ctx.cookie("token"); // jws
        if(token == null) {
            reject(ctx, HttpStatus.UNAUTHORIZED, "Token is missing.");
            return;
        }

        final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
        if(!JwtManager.verifyToken(token, hashedRealKey, plugin.getConfig().salt)) {
            ctx.removeCookie("token");
            reject(ctx, HttpStatus.UNAUTHORIZED, "Token is invalid.");
        }
    };

    public Handler handleRsc = ctx -> {
        if(!ctx.path().endsWith(".txt") || !"1".equals(ctx.header("Rsc"))) return;

        ctx.contentType("text/x-component");
        if(rscCompatibilityId != null) {
            ctx.header("X-Vinext-RSC-Compatibility-Id", rscCompatibilityId);
        }
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
            ctx.skipRemainingHandlers();
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
            ctx.skipRemainingHandlers();
        }
    };

    public Handler routeExtensionBackend = ctx -> {
        String extensionId = ctx.pathParam("extId");
        String path = ctx.pathParamMap().containsKey("path") ? ctx.pathParam("path") : "";
        String normalizedPath = path.isEmpty() ? "index.html" : Utils.normalizePath(path);
        if(normalizedPath == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid extension backend path.");
            return;
        }

        ExtensionManager extensionManager = plugin.getExtensionManager();
        if(!extensionManager.hasExtension(extensionId)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension not found.");
            return;
        }

        LoadedExtension extension = extensionManager.getExtension(extensionId);
        LoadedExtension.BackendRoute route = extension.getBackendRoute(normalizedPath);
        if(route == null || !route.method().equals(ctx.method())) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension backend path not found.");
            return;
        }

        Thread thread = Thread.currentThread();
        ClassLoader previousClassLoader = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(extension.classLoader);
            route.handler().handle(ctx);
        } finally {
            thread.setContextClassLoader(previousClassLoader);
        }
    };

    private boolean isManagedAuthPath(String path) {
        return path.equals("/api") || path.startsWith("/api/")
                || path.equals("/assets/upload") || path.startsWith("/assets/upload/")
                || path.equals("/assets/reset") || path.startsWith("/assets/reset/")
                || path.equals("/file") || path.startsWith("/file/");
    }

    private boolean constantTimeEquals(String actual, String expected) {
        if(actual == null || expected == null) return false;
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void reject(Context ctx, HttpStatus status, String message) {
        sendResponse(ctx, status, message);
        ctx.skipRemainingHandlers();
    }

    private String getOpenAPIInterfaceName(String path) {
        final String prefix = "/open-api/";
        if(path == null || !path.startsWith(prefix)) return null;

        String routePath = path.substring(prefix.length());
        if(routePath.isEmpty()) return null;

        int splitIndex = routePath.indexOf('/');
        if(splitIndex == -1) return routePath;

        return routePath.substring(0, splitIndex);
    }

}
