package net.opanel.controller.api;

import io.javalin.http.*;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.controller.BaseController;
import net.opanel.exception.ActLaterException;
import net.opanel.utils.Callback;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PluginsController extends BaseController {
    private final DownloadController downloadController = getControllerInstance(DownloadController.class);
    private final ConcurrentHashMap<String, PendingOperation> pendingOperationMap = new ConcurrentHashMap<>();

    enum PendingOperation {
        ENABLED, DISABLED, DELETED;

        boolean isEnabled() { return this == PendingOperation.ENABLED; }
    }

    public PluginsController(OPanel plugin) {
        super(plugin);
    }

    public Handler getPlugins = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();

        List<HashMap<String, Object>> plugins = new ArrayList<>();
        for(OPanelPlugin p : server.getPlugins()) {
            PendingOperation futureStatus = pendingOperationMap.get(p.getFileName());
            if(futureStatus == PendingOperation.DELETED) continue;

            final String description = p.getDescription();

            HashMap<String, Object> pluginInfo = new HashMap<>();
            pluginInfo.put("fileName", Utils.stringToBase64(p.getFileName()));
            pluginInfo.put("name", p.getName());
            pluginInfo.put("version", p.getVersion());
            pluginInfo.put("description", description != null ? Utils.stringToBase64(description) : null);
            pluginInfo.put("authors", p.getAuthors());
            pluginInfo.put("website", p.getWebsite());
            pluginInfo.put("icon", p.getIcon() != null ? "/api/plugins/icon/"+ p.getFileName() +"?t="+ System.currentTimeMillis() : null);
            pluginInfo.put("size", p.getFileSize());
            pluginInfo.put("enabled", futureStatus != null ? futureStatus.isEnabled() : p.isEnabled());
            pluginInfo.put("loaded", p.isLoaded());
            plugins.add(pluginInfo);
        }
        obj.put("plugins", plugins);
        obj.put("folderPath", server.getPluginsPath().toAbsolutePath().toString());

        sendResponse(ctx, obj);
    };

    public Handler getPluginIcon = ctx -> {
        final String fileName = ctx.pathParam("fileName");
        if(!isValidPluginFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        for(OPanelPlugin plugin : server.getPlugins()) {
            if(fileName.equals(plugin.getFileName())) {
                if(!plugin.isLoaded()) {
                    sendResponse(ctx, HttpStatus.PRECONDITION_FAILED, "The plugin is not loaded by the server.");
                    return;
                }

                byte[] icon = plugin.getIcon();
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

    public Handler uploadPlugin = ctx -> {
        try {
            UploadedFile file = ctx.uploadedFile("file");
            if(file == null || file.size() <= 0) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "File is missing.");
                return;
            }

            final String fileName = file.filename();
            if(!Utils.isSafeFileName(fileName)) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
                return;
            }
            if(!fileName.endsWith(".jar")) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Plugin file should be a .jar file.");
                return;
            }

            final Path pluginsPath = server.getPluginsPath();
            final Path targetPath = pluginsPath.resolve(fileName);
            
            // Check if file already exists (either enabled or disabled)
            if(Files.exists(targetPath) || Files.exists(pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX))) {
                sendResponse(ctx, HttpStatus.CONFLICT, "Plugin file already exists.");
                return;
            }

            // Copy file to plugins directory
            try(InputStream is = file.content()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            sendResponse(ctx, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler togglePlugin = ctx -> {
        final String fileName = ctx.pathParam("fileName");
        final String enabled = ctx.queryParam("enabled");
        if(!isValidPluginFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        if(enabled == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Enabled status is missing.");
            return;
        }

        try {
            server.togglePlugin(fileName, enabled.equals("1"));
            sendResponse(ctx, HttpStatus.OK);
        } catch (ActLaterException e) {
            pendingOperationMap.put(fileName, enabled.equals("1") ? PendingOperation.ENABLED : PendingOperation.DISABLED);
            sendResponse(ctx, HttpStatus.ACCEPTED);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the plugin.");
        } catch (IllegalStateException e) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Cannot disable a loaded plugin.");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler deletePlugin = ctx -> {
        final String fileName = ctx.pathParam("fileName");
        if(!isValidPluginFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        try {
            server.deletePlugin(fileName);
            sendResponse(ctx, HttpStatus.OK);
        } catch (ActLaterException e) {
            pendingOperationMap.put(fileName, PendingOperation.DELETED);
            sendResponse(ctx, HttpStatus.ACCEPTED);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the plugin.");
        } catch (IllegalStateException e) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Cannot delete a loaded plugin.");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler downloadPlugin = ctx -> {
        final String fileName = ctx.pathParam("fileName");
        if(!isValidPluginFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        final Path pluginsPath = server.getPluginsPath();
        Path filePath = pluginsPath.resolve(fileName);
        
        // Also check for disabled version
        if(!Files.exists(filePath)) {
            filePath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        }
        
        if(!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the plugin.");
            return;
        }

        final String downloadId = downloadController.registerPath(filePath);
        ctx.redirect("/file/"+ downloadId +"/"+ fileName.replaceAll("\\"+ OPanelPlugin.DISABLED_SUFFIX +"$", ""));
    };

    private boolean isValidPluginFileName(String fileName) {
        return Utils.isSafeFileName(fileName)
                && (fileName.endsWith(".jar") || fileName.endsWith(".jar"+ OPanelPlugin.DISABLED_SUFFIX));
    }
}
