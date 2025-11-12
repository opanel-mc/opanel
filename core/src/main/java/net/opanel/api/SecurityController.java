package net.opanel.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.config.OPanelConfiguration;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.util.HashMap;

public class SecurityController extends BaseController {
    public static final String route = "/api/security";

    public SecurityController(OPanel plugin) {
        super(plugin);
    }

    public Handler updateSecurity = ctx -> {
        // 验证authCookie
        if (!authCookie(ctx)) {
            sendResponse(ctx, 401);
            return;
        }

        // 解析请求体
        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if (reqBody.currentKey == null || reqBody.newKey == null) {
            sendResponse(ctx, 400);
            return;
        }

        final String currentKey = reqBody.currentKey; // hashed 1
        final String newKey = reqBody.newKey; // hashed 1
        final String realKey = plugin.getConfig().accessKey; // hashed 2

        // 验证currentKey
        if (!Utils.md5(currentKey).equals(realKey)) {
            sendResponse(ctx, 403);
            return;
        }

        // 保存新的access key
        OPanelConfiguration config = plugin.getConfig();
        config.accessKey = Utils.md5(newKey);
        plugin.setConfig(config);

        // 发送新的token
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("token", Utils.md5(config.salt + config.accessKey));
        sendResponse(ctx, obj);
    };

    private static class RequestBodyType {
        public String currentKey;
        public String newKey;
    }
}