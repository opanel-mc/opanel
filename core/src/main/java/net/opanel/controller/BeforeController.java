package net.opanel.controller;

import io.javalin.http.*;
import io.javalin.http.servlet.JavalinServletContext;
import net.opanel.OPanel;
import net.opanel.config.McpConfiguration;
import net.opanel.config.OpenAPIConfiguration;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.web.JwtManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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

    public Handler handleRsc = ctx -> {
        String reqPath = ctx.path();
        Map<String, List<String>> queryParamMap = ctx.queryParamMap();
        if(reqPath.endsWith(".rsc")) {
            if(rscCompatibilityId != null) {
                ctx.header("X-Vinext-RSC-Compatibility-Id", rscCompatibilityId);
            }
            return;
        }
        if(!queryParamMap.containsKey("_rsc")) return;

        if(reqPath.endsWith("/")) {
            reqPath = reqPath.substring(0, reqPath.length() - 1);
        }
        ctx.redirect((reqPath.isEmpty() ? "index" : reqPath) +".rsc?"+ ctx.queryString());
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
