package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.config.OPanelConfiguration;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;
import net.opanel.web.JwtManager;

import java.util.HashMap;

public class SecurityController extends BaseController {
    public SecurityController(OPanel plugin) {
        super(plugin);
    }

    public Handler updateAccessKey = ctx -> {
        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if(reqBody == null || reqBody.currentKey == null || reqBody.newKey == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid request body.");
            return;
        }

        final String currentKey = reqBody.currentKey; // hashed 1
        final String newKey = reqBody.newKey; // hashed 1
        final String realKey = plugin.getConfig().accessKey; // hashed 2

        if(!Utils.md5(currentKey).equals(realKey)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Access key mismatch.");
            return;
        }

        // Save new access key
        OPanelConfiguration config = plugin.getConfig();
        config.accessKey = Utils.md5(newKey);
        plugin.setConfig(config);

        // Send new token
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("token", JwtManager.generateToken(config.accessKey, config.salt));
        sendResponse(ctx, obj);
    };

    private static class RequestBodyType {
        String currentKey;
        String newKey;
    }
}
