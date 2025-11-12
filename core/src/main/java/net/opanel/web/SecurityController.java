package net.opanel.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.config.OPanelConfiguration;
import net.opanel.utils.Utils;

import java.util.HashMap;

public class SecurityController extends BaseController {
    public static final String route = "/api/security";

    public SecurityController(OPanel plugin) {
        super(plugin);
    }

    public final Handler updateSecurity = ctx -> {
        // Auth cookie validation
        if (!authCookie(ctx)) {
            sendResponse(ctx, io.javalin.http.HttpStatus.UNAUTHORIZED);
            return;
        }

        // Parse request body
        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if (reqBody.currentKey == null || reqBody.newKey == null) {
            sendResponse(ctx, io.javalin.http.HttpStatus.BAD_REQUEST);
            return;
        }

        final String currentKey = reqBody.currentKey; // hashed 1
        final String newKey = reqBody.newKey; // hashed 1
        final String realKey = plugin.getConfig().accessKey; // hashed 2

        // Validate current key
        if (!Utils.md5(currentKey).equals(realKey)) {
            sendResponse(ctx, io.javalin.http.HttpStatus.FORBIDDEN);
            return;
        }

        // Save new access key
        OPanelConfiguration config = plugin.getConfig();
        config.accessKey = Utils.md5(newKey);
        plugin.setConfig(config);

        // Send new token
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("token", Utils.md5(config.salt + config.accessKey));
        sendResponse(ctx, obj);
    };

    private static class RequestBodyType {
        public String currentKey;
        public String newKey;
    }

    // Helper method to validate auth cookie
    private boolean authCookie(Context ctx) {
        // Implementation based on BaseServlet's authCookie method
        // This should check for the authentication cookie
        String authCookie = ctx.cookie("auth");
        return authCookie != null && authCookie.equals("true"); // Simplified - actual implementation may vary
    }
}