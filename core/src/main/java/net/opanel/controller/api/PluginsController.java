package net.opanel.controller.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.javalin.http.*;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.config.OPanelConfiguration;
import net.opanel.controller.BaseController;
import net.opanel.exception.ActLaterException;
import net.opanel.exception.PluginUpdateConflictException;
import net.opanel.update.PluginUpdate;
import net.opanel.update.PluginUpdateBinding;
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
import java.util.Map;
import java.util.Set;
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
        Map<String, PluginUpdate> cachedUpdates = new HashMap<>();
        for(PluginUpdate update : plugin.getPluginUpdateManager().getCoordinator().getCachedUpdates()) {
            cachedUpdates.putIfAbsent(update.getFileName(), update);
        }

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
            PluginUpdateBinding binding = plugin.getPluginUpdateManager().getCoordinator().getBinding(p.getFileName());
            PluginUpdate cachedUpdate = cachedUpdates.get(p.getFileName());
            pluginInfo.put("source", toDisplaySource(binding != null ? binding.getSource() : cachedUpdate == null ? null : cachedUpdate.getSource()));
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

            plugin.getPluginUpdateManager().invalidateCache();
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
            plugin.getPluginUpdateManager().invalidateCache();
            sendResponse(ctx, HttpStatus.OK);
        } catch (ActLaterException e) {
            pendingOperationMap.put(fileName, enabled.equals("1") ? PendingOperation.ENABLED : PendingOperation.DISABLED);
            plugin.getPendingPluginOperations().add(fileName);
            plugin.getPluginUpdateManager().invalidateCache();
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
            plugin.getPluginUpdateManager().invalidateCache();
            sendResponse(ctx, HttpStatus.OK);
        } catch (ActLaterException e) {
            pendingOperationMap.put(fileName, PendingOperation.DELETED);
            plugin.getPendingPluginOperations().add(fileName);
            plugin.getPluginUpdateManager().invalidateCache();
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

            // The auto check can be disabled by the configuration, while the manual
            // check (force) always works
            if(!force && !plugin.getConfig().autoCheckPluginUpdates) {
                HashMap<String, Object> obj = new HashMap<>();
                obj.put("updates", new ArrayList<>());
                sendResponse(ctx, obj);
                return;
            }

            final List<PluginUpdate> updates = plugin.getPluginUpdateManager().check(
                server.getPluginsPath(),
                getPluginsWithoutPendingOperations(),
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
                updateInfo.put("source", toDisplaySource(update.getSource()));
                updateInfo.put("projectId", update.getProjectId());
                updateInfo.put("requiresBinding", update.isRequiresBinding());
                updateInfo.put("requiresRestart", update.isRequiresRestart());
                updateInfo.put("channel", update.getChannel());
                updateInfo.put("digestAlgorithm", update.getDigestAlgorithm());
                updateInfo.put("digestValue", update.getDigestValue());
                updateList.add(updateInfo);
            }
            obj.put("updates", updateList);
            sendResponse(ctx, obj);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, "Failed to check plugin updates.");
        }
    };

    public Handler getPluginUpdateBindings = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        List<Map<String, Object>> bindings = new ArrayList<>();
        for(PluginUpdateBinding binding : plugin.getPluginUpdateManager().getCoordinator().getBindingsSnapshot().values()) {
            Map<String, Object> bindingInfo = new HashMap<>();
            bindingInfo.put("fileName", Utils.stringToBase64(binding.getFileName()));
            bindingInfo.put("source", binding.getSource());
            bindingInfo.put("projectId", binding.getProjectId());
            bindingInfo.put("owner", binding.getOwner());
            bindingInfo.put("repo", binding.getRepo());
            bindingInfo.put("assetPattern", binding.getAssetPattern());
            bindingInfo.put("channels", binding.getChannels() == null ? List.of() : binding.getChannels());
            bindings.add(bindingInfo);
        }
        obj.put("bindings", bindings);
        sendResponse(ctx, obj);
    };

    public Handler setPluginUpdateBinding = ctx -> {
        try {
            final JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            final String fileName = body.has("fileName") && !body.get("fileName").isJsonNull()
                ? body.get("fileName").getAsString()
                : null;
            if(fileName == null || !isValidPluginFileName(fileName)) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
                return;
            }

            final String source = body.has("source") && !body.get("source").isJsonNull()
                ? body.get("source").getAsString()
                : null;
            if(source == null || source.isBlank()) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Source is required.");
                return;
            }

            final String projectId = body.has("projectId") && !body.get("projectId").isJsonNull()
                ? body.get("projectId").getAsString()
                : null;
            final String owner = body.has("owner") && !body.get("owner").isJsonNull()
                ? body.get("owner").getAsString()
                : null;
            final String repo = body.has("repo") && !body.get("repo").isJsonNull()
                ? body.get("repo").getAsString()
                : null;
            final String assetPattern = body.has("assetPattern") && !body.get("assetPattern").isJsonNull()
                ? body.get("assetPattern").getAsString()
                : null;

            List<String> channels = null;
            if(body.has("channels") && body.get("channels").isJsonArray()) {
                channels = new ArrayList<>();
                for(JsonElement element : body.getAsJsonArray("channels")) {
                    channels.add(element.getAsString());
                }
            }

            plugin.getPluginUpdateManager().getCoordinator().setBinding(new PluginUpdateBinding(
                fileName,
                source,
                projectId,
                owner,
                repo,
                assetPattern,
                channels
            ));
            plugin.getPluginUpdateManager().invalidateCache();
            getPluginUpdateBindings.handle(ctx);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal request body.");
        }
    };

    public Handler removePluginUpdateBinding = ctx -> {
        final String fileName = ctx.queryParam("fileName");
        if(fileName == null || !isValidPluginFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        plugin.getPluginUpdateManager().getCoordinator().removeBinding(fileName);
        plugin.getPluginUpdateManager().invalidateCache();
        getPluginUpdateBindings.handle(ctx);
    };

    public Handler getPluginUpdateStatus = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("autoCheckPluginUpdates", plugin.getConfig().autoCheckPluginUpdates);
        obj.put("autoApplyPluginUpdates", plugin.getConfig().autoApplyPluginUpdates);
        obj.put("pluginUpdateRestartStrategy", plugin.getConfig().pluginUpdateRestartStrategy);
        obj.put("pluginUpdateCheckInterval", plugin.getConfig().pluginUpdateCheckInterval);
        obj.put("lastCheckedAt", plugin.getPluginUpdateManager().getCoordinator().getLastCheckedAt());
        obj.put("pendingUpdateCount", plugin.getPluginUpdateManager().getCoordinator().getCachedUpdates().size());
        sendResponse(ctx, obj);
    };

    public Handler updatePluginSettings = ctx -> {
        try {
            final JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

            final OPanelConfiguration config = plugin.getConfig();
            if(body.has("autoCheckPluginUpdates") && body.get("autoCheckPluginUpdates").isJsonPrimitive()) {
                config.autoCheckPluginUpdates = body.get("autoCheckPluginUpdates").getAsBoolean();
            }
            if(body.has("autoApplyPluginUpdates") && body.get("autoApplyPluginUpdates").isJsonPrimitive()) {
                config.autoApplyPluginUpdates = body.get("autoApplyPluginUpdates").getAsBoolean();
            }
            if(body.has("pluginUpdateRestartStrategy") && body.get("pluginUpdateRestartStrategy").isJsonPrimitive()
                && !body.get("pluginUpdateRestartStrategy").getAsString().isBlank()) {
                config.pluginUpdateRestartStrategy = body.get("pluginUpdateRestartStrategy").getAsString();
            }
            if(body.has("pluginUpdateCheckInterval") && body.get("pluginUpdateCheckInterval").isJsonPrimitive()) {
                final int interval = body.get("pluginUpdateCheckInterval").getAsInt();
                if(interval >= 60) {
                    config.pluginUpdateCheckInterval = interval;
                }
            }

            plugin.setConfig(config);
            plugin.getPluginUpdateManager().invalidateCache();
            getPluginUpdateStatus.handle(ctx);
        } catch (JsonParseException | UnsupportedOperationException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal request body.");
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
            final List<PluginUpdate> updates = plugin.getPluginUpdateManager().check(
                server.getPluginsPath(),
                getPluginsWithoutPendingOperations(),
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

            plugin.getPluginUpdateManager().update(server, toUpdate);
            sendResponse(ctx, HttpStatus.ACCEPTED);
        } catch (ActLaterException e) {
            sendResponse(ctx, HttpStatus.ACCEPTED);
        } catch (PluginUpdateConflictException e) {
            sendResponse(ctx, HttpStatus.CONFLICT, e.getMessage());
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the plugin.");
        } catch (IllegalStateException e) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Cannot update a loaded plugin.");
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    private boolean isValidPluginFileName(String fileName) {
        return Utils.isSafeFileName(fileName)
                && (fileName.endsWith(".jar") || fileName.endsWith(".jar"+ OPanelPlugin.DISABLED_SUFFIX));
    }

    private static String toDisplaySource(String source) {
        if(source == null || source.isBlank()) return "unbound";
        if("modrinth".equals(source) || "curseforge".equals(source) || "mcim".equals(source)) {
            return "mcim";
        }
        if("hangar".equals(source) || "github".equals(source)) return source;
        return "unbound";
    }

    private List<OPanelPlugin> getPluginsWithoutPendingOperations() {
        List<OPanelPlugin> candidates = new ArrayList<>();
        for(OPanelPlugin plugin : server.getPlugins()) {
            // The file can still exist on disk while an enable / disable / delete
            // operation is deferred until restart. Exclude it from both update
            // discovery and update execution to avoid competing file operations.
            if(pendingOperationMap.containsKey(plugin.getFileName())) continue;
            candidates.add(plugin);
        }
        return candidates;
    }
}
