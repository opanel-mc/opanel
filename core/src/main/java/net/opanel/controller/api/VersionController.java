package net.opanel.controller.api;

import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.config.MapConfiguration;
import net.opanel.controller.BaseController;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;

import java.util.HashMap;

public class VersionController extends BaseController {
    private MapConfiguration mapConfig;

    public VersionController(OPanel plugin) {
        super(plugin);

        mapConfig = Storage.get().getStoredData(StorageKey.MAP_CONFIG);
    }

    public Handler getVersionInfo = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("serverType", server.getServerType().getName());
        obj.put("version", server.getVersion());
        obj.put("map", mapConfig.enabled);
        obj.put("mcdr", OPanel.isMCDRBridgeActive());
        sendResponse(ctx, obj);
    };
}
