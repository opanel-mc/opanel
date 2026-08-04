package net.opanel.controller.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.javalin.http.*;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.controller.BaseController;
import net.opanel.exception.ActLaterException;
import net.opanel.update.PluginUpdate;
import net.opanel.update.PluginUpdateConflictException;
import net.opanel.update.PluginUpdateManager;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PluginsController extends BaseController {
    private final DownloadController downloadController = getControllerInstance(DownloadController.class);
    private final PluginUpdateManager pluginUpdateManager = new PluginUpdateManager();
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

            pluginUpdateManager.invalidateCache();
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
            pluginUpdateManager.invalidateCache();
            sendResponse(ctx, HttpStatus.OK);
        } catch (ActLaterException e) {
            pendingOperationMap.put(fileName, enabled.equals("1") ? PendingOperation.ENABLED : PendingOperation.DISABLED);
            pluginUpdateManager.invalidateCache();
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
            pluginUpdateManager.invalidateCache();
            sendResponse(ctx, HttpStatus.OK);
        } catch (ActLaterException e) {
            pendingOperationMap.put(fileName, PendingOperation.DELETED);
            pluginUpdateManager.invalidateCache();
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

    public Handler checkPluginUpdates = ctx -> {
        try {
            final boolean force = "1".equals(ctx.queryParam("force"));
            final List<PluginUpdate> updates = pluginUpdateManager.check(
                server.getPluginsPath(),
                server.getPlugins(),
                server.getVersion(),
                server.getServerType(),
                force
            );

            HashMap<String, Object> obj = new HashMap<>();
            List<HashMap<String, Object>> updateList = new ArrayList<>();
            for(PluginUpdate update : updates) {
                HashMap<String, Object> updateInfo = new HashMap<>();
                updateInfo.put("fileName", Utils.stringToBase64(update.getFileName()));
                updateInfo.put("name", update.getName());
                updateInfo.put("currentVersion", update.getCurrentVersion());
                updateInfo.put("latestVersion", update.getLatestVersion());
                updateInfo.put("downloadUrl", update.getDownloadUrl());
                updateInfo.put("projectUrl", update.getProjectUrl());
                updateList.add(updateInfo);
            }
            obj.put("updates", updateList);
            sendResponse(ctx, obj);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, "Failed to check plugin updates.");
        }
    };

    public Handler updatePlugins = ctx -> {
        final Set<String> fileNames = new LinkedHashSet<>();
        try {
            final JsonArray body = JsonParser.parseString(ctx.body()).getAsJsonArray();
            for(JsonElement element : body) {
                final String fileName = element.getAsString();
                if(!isValidPluginFileName(fileName)) {
                    sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
                    return;
                }
                fileNames.add(fileName);
            }
            if(fileNames.isEmpty()) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "File name is missing.");
                return;
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal request body.");
            return;
        }

        try {
            final List<PluginUpdate> updates = pluginUpdateManager.check(
                server.getPluginsPath(),
                server.getPlugins(),
                server.getVersion(),
                server.getServerType(),
                false
            );

            final List<PluginUpdate> toUpdate = new ArrayList<>();
            for(PluginUpdate update : updates) {
                if(fileNames.contains(update.getFileName())) {
                    toUpdate.add(update);
                }
            }
            if(toUpdate.size() != fileNames.size()) {
                sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the update info of the plugin.");
                return;
            }

            pluginUpdateManager.update(server, toUpdate);
            sendResponse(ctx, HttpStatus.ACCEPTED);
        } catch (ActLaterException e) {
            sendResponse(ctx, HttpStatus.ACCEPTED);
        } catch (PluginUpdateConflictException e) {
            sendResponse(ctx, HttpStatus.CONFLICT, e.getMessage());
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the plugin.");
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    private boolean isValidPluginFileName(String fileName) {
        return Utils.isSafeFileName(fileName)
                && (fileName.endsWith(".jar") || fileName.endsWith(".jar"+ OPanelPlugin.DISABLED_SUFFIX));
    }
}
