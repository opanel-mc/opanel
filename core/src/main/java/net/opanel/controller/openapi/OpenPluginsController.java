package net.opanel.controller.openapi;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.controller.BaseController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenPluginsController extends BaseController {
    public OpenPluginsController(OPanel plugin) {
        super(plugin);
    }

    public Handler getPlugins = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();

        List<HashMap<String, Object>> plugins = new ArrayList<>();
        for(OPanelPlugin p : server.getPlugins()) {
            final String description = p.description();

            HashMap<String, Object> pluginInfo = new HashMap<>();
            pluginInfo.put("fileName", p.fileName());
            pluginInfo.put("name", p.name());
            pluginInfo.put("version", p.version());
            pluginInfo.put("description", description);
            pluginInfo.put("authors", p.authors());
            pluginInfo.put("website", p.website());
            pluginInfo.put("icon", p.icon() != null ? "/open-api/plugins/icon/"+ p.fileName() : null);
            pluginInfo.put("size", p.fileSize());
            pluginInfo.put("enabled", p.enabled());
            pluginInfo.put("loaded", p.loaded());
            plugins.add(pluginInfo);
        }
        obj.put("plugins", plugins);

        sendResponse(ctx, obj);
    };

    public Handler getPluginIcon = ctx -> {
        final String fileName = ctx.pathParam("fileName");
        if(!fileName.endsWith(".jar") && !fileName.endsWith(".jar"+ OPanelPlugin.DISABLED_SUFFIX)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        for(OPanelPlugin plugin : server.getPlugins()) {
            if(fileName.equals(plugin.fileName())) {
                if(!plugin.loaded()) {
                    sendResponse(ctx, HttpStatus.PRECONDITION_FAILED, "The plugin is not loaded by the server.");
                    return;
                }

                byte[] icon = plugin.icon();
                if(icon == null) {
                    sendResponse(ctx, HttpStatus.UNPROCESSABLE_CONTENT, "The plugin doesn't have an icon.");
                    return;
                }

                sendContent(ctx, icon, ContentType.IMAGE_PNG);
                return;
            }
        }

        sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the plugin.");
    };
}
