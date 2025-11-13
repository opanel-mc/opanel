package net.opanel.api;

import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.web.BaseController;

import java.util.HashMap;

public class VersionController extends BaseController {
    public VersionController(OPanel plugin) {
        super(plugin);
    }

    public Handler getVersionInfo = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("serverType", server.getServerType().getName());
        obj.put("version", server.getVersion());
        sendResponse(ctx, obj);
    };
}
