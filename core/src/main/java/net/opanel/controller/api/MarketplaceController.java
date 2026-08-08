package net.opanel.controller.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.ServerType;
import net.opanel.controller.BaseController;
import net.opanel.exception.MarketplaceInstallConflictException;
import net.opanel.exception.MarketplaceRateLimitException;
import net.opanel.update.MarketplaceService;
import net.opanel.update.ModrinthApi;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * HTTP entry point for the plugin/mod marketplace.
 *
 * <p>Everything server-side reads the Modrinth catalog through
 * {@link MarketplaceService}; the active mirror preference comes from the same
 * {@code modrinthApiSource} config that drives update detection.</p>
 */
public class MarketplaceController extends BaseController {
    // Initialized at the field declaration: subclass field initializers run
    // after the superclass constructor has assigned `plugin`.
    private final MarketplaceService marketplaceService = new MarketplaceService(plugin);

    public MarketplaceController(OPanel plugin) {
        super(plugin);
    }

    private void syncSource() {
        marketplaceService.setSource(plugin.getConfig().modrinthApiSource);
    }

    private String mcVersion() {
        return ModrinthApi.extractMcVersion(server.getVersion());
    }

    public Handler getStatus = ctx -> {
        syncSource();
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("source", plugin.getConfig().modrinthApiSource);
        obj.put("serverType", server.getServerType().getName());
        obj.put("mcVersion", mcVersion());
        obj.put("loaderCategories", ModrinthApi.modrinthLoaders(server.getServerType()));
        sendResponse(ctx, obj);
    };

    public Handler search = ctx -> {
        syncSource();
        try {
            final String query = ctx.queryParam("q");
            final String category = ctx.queryParam("category");
            final ServerType platform = parsePlatform(ctx.queryParam("platform"));
            final String gameVersion = ctx.queryParam("gameVersion");
            final boolean compatibleOnly = !"0".equals(ctx.queryParam("compatibleOnly"));
            final String sort = ctx.queryParam("sort");
            final int offset = parseInt(ctx.queryParam("offset"), 0);
            final int limit = parseInt(ctx.queryParam("limit"), 20);

            final MarketplaceService.SearchResult result = marketplaceService.search(
                query, category, platform, gameVersion, compatibleOnly, sort, offset, limit
            );

            HashMap<String, Object> obj = new HashMap<>();
            List<HashMap<String, Object>> hits = new ArrayList<>();
            for(MarketplaceService.ProjectHit hit : result.getHits()) {
                HashMap<String, Object> h = new HashMap<>();
                h.put("id", hit.getId());
                h.put("slug", hit.getSlug());
                h.put("title", Utils.stringToBase64(hit.getTitle()));
                h.put("author", Utils.stringToBase64(hit.getAuthor()));
                h.put("summary", Utils.stringToBase64(hit.getSummary()));
                h.put("iconUrl", hit.getIconUrl());
                h.put("downloads", hit.getDownloads());
                h.put("follows", hit.getFollows());
                h.put("projectUrl", hit.getProjectUrl());
                h.put("projectType", hit.getProjectType());
                h.put("categories", hit.getCategories());
                h.put("updatedAt", hit.getUpdatedAt());
                hits.add(h);
            }
            obj.put("hits", hits);
            obj.put("totalHits", result.getTotalHits());
            obj.put("offset", result.getOffset());
            obj.put("limit", result.getLimit());

            HashMap<String, Object> applied = new HashMap<>();
            applied.put("serverType", server.getServerType().getName());
            applied.put("mcVersion", mcVersion());
            applied.put("source", plugin.getConfig().modrinthApiSource);
            applied.put("compatibleOnly", compatibleOnly);
            obj.put("applied", applied);
            sendResponse(ctx, obj);
        } catch (MarketplaceRateLimitException e) {
            sendResponse(ctx, HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    };

    public Handler getProject = ctx -> {
        syncSource();
        try {
            final String projectId = ctx.pathParam("projectId");
            final MarketplaceService.ProjectDetail detail = marketplaceService.getProject(projectId);

            HashMap<String, Object> obj = new HashMap<>();
            obj.put("project", projectToMap(detail));
            List<HashMap<String, Object>> versions = new ArrayList<>();
            for(MarketplaceService.MarketplaceVersion version : detail.getVersions()) {
                versions.add(versionToMap(version));
            }
            obj.put("versions", versions);
            obj.put("versionsFilteredByGame", detail.isVersionsFilteredByGame());
            obj.put("compatibility", compatibilityMap());
            sendResponse(ctx, obj);
        } catch (MarketplaceRateLimitException e) {
            sendResponse(ctx, HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    };

    public Handler getInstallPreview = ctx -> {
        syncSource();
        try {
            final String projectId = ctx.pathParam("projectId");
            final String versionId = ctx.queryParam("versionId");
            final MarketplaceService.InstallPreview preview = marketplaceService.previewInstall(projectId, versionId);

            HashMap<String, Object> obj = new HashMap<>();
            obj.put("target", selectedFileToMap(preview.getTarget()));
            List<HashMap<String, Object>> missing = new ArrayList<>();
            for(MarketplaceService.SelectedFile file : preview.getMissingDependencies()) {
                missing.add(selectedFileToMap(file));
            }
            obj.put("missingDependencies", missing);
            obj.put("alreadyInstalled", dependenciesToMap(preview.getAlreadyInstalled()));
            obj.put("unresolvedDependencies", dependenciesToMap(preview.getUnresolvedDependencies()));
            obj.put("conflicts", targetConflicts(preview.getTarget()));
            sendResponse(ctx, obj);
        } catch (MarketplaceRateLimitException e) {
            sendResponse(ctx, HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    };

    public Handler install = ctx -> {
        syncSource();
        try {
            final List<MarketplaceService.InstallEntry> entries = parseInstallBody(ctx.body());
            final MarketplaceService.InstallResult result = marketplaceService.install(entries);

            HashMap<String, Object> obj = new HashMap<>();
            List<HashMap<String, Object>> installed = new ArrayList<>();
            for(MarketplaceService.SelectedFile file : result.getInstalled()) {
                HashMap<String, Object> m = new HashMap<>();
                m.put("projectId", file.getProjectId());
                m.put("projectTitle", Utils.stringToBase64(file.getProjectTitle()));
                m.put("versionNumber", Utils.stringToBase64(file.getVersionNumber()));
                m.put("fileName", file.getFileName());
                installed.add(m);
            }
            obj.put("installed", installed);
            obj.put("requiresRestart", result.isRequiresRestart());
            sendResponse(ctx, obj);
        } catch (MarketplaceInstallConflictException e) {
            sendResponse(ctx, HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (JsonParseException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal request body.");
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    private List<HashMap<String, Object>> targetConflicts(MarketplaceService.SelectedFile target) {
        final List<HashMap<String, Object>> conflicts = new ArrayList<>();
        final Path pluginsPath = server.getPluginsPath();
        if(
            Files.exists(pluginsPath.resolve(target.getFileName()))
            || Files.exists(pluginsPath.resolve(target.getFileName() + OPanelPlugin.DISABLED_SUFFIX))
        ) {
            HashMap<String, Object> conflict = new HashMap<>();
            conflict.put("fileName", target.getFileName());
            conflicts.add(conflict);
        }
        return conflicts;
    }

    private static List<MarketplaceService.InstallEntry> parseInstallBody(String body) {
        final JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if(!json.has("entries") || !json.get("entries").isJsonArray()) {
            throw new IllegalArgumentException("Entries are required.");
        }
        final List<MarketplaceService.InstallEntry> entries = new ArrayList<>();
        final JsonArray array = json.getAsJsonArray("entries");
        for(JsonElement element : array) {
            if(!element.isJsonObject()) continue;
            final JsonObject entry = element.getAsJsonObject();
            if(!entry.has("projectId") || !entry.has("versionId")) continue;
            entries.add(new MarketplaceService.InstallEntry(
                entry.get("projectId").getAsString(),
                entry.get("versionId").getAsString()
            ));
        }
        if(entries.isEmpty()) {
            throw new IllegalArgumentException("Entries are required.");
        }
        return entries;
    }

    private HashMap<String, Object> projectToMap(MarketplaceService.ProjectDetail detail) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("id", detail.getId());
        m.put("slug", detail.getSlug());
        m.put("title", Utils.stringToBase64(detail.getTitle()));
        m.put("author", Utils.stringToBase64(detail.getAuthor()));
        m.put("description", Utils.stringToBase64(detail.getDescription()));
        m.put("iconUrl", detail.getIconUrl());
        m.put("projectUrl", detail.getProjectUrl());
        m.put("sourceUrl", detail.getSourceUrl());
        m.put("projectType", detail.getProjectType());
        m.put("updatedAt", detail.getUpdatedAt());
        m.put("downloads", detail.getDownloads());
        m.put("follows", detail.getFollows());
        m.put("categories", detail.getCategories());
        m.put("versionIds", detail.getVersionIds());
        return m;
    }

    private HashMap<String, Object> versionToMap(MarketplaceService.MarketplaceVersion version) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("id", version.getId());
        m.put("name", Utils.stringToBase64(version.getName()));
        m.put("versionNumber", Utils.stringToBase64(version.getVersionNumber()));
        m.put("channel", version.getChannel());
        m.put("datePublished", version.getDatePublished());
        m.put("gameVersions", version.getGameVersions());
        m.put("loaders", version.getLoaders());
        m.put("downloads", version.getDownloads());
        m.put("fileName", version.getFileName());
        m.put("fileSize", version.getFileSize());
        m.put("downloadUrl", version.getDownloadUrl());
        m.put("sha1", version.getSha1());
        m.put("compatible", version.isCompatible());
        m.put("dependencies", dependenciesToMap(version.getDependencies()));
        return m;
    }

    private HashMap<String, Object> selectedFileToMap(MarketplaceService.SelectedFile file) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("projectId", file.getProjectId());
        m.put("projectTitle", Utils.stringToBase64(file.getProjectTitle()));
        m.put("versionId", file.getVersionId());
        m.put("versionNumber", Utils.stringToBase64(file.getVersionNumber()));
        m.put("fileName", file.getFileName());
        m.put("size", file.getSize());
        m.put("url", file.getUrl());
        m.put("sha1", file.getSha1());
        return m;
    }

    private List<HashMap<String, Object>> dependenciesToMap(List<MarketplaceService.DependencyInfo> dependencies) {
        List<HashMap<String, Object>> result = new ArrayList<>();
        for(MarketplaceService.DependencyInfo dependency : dependencies) {
            HashMap<String, Object> m = new HashMap<>();
            m.put("projectId", dependency.getProjectId());
            m.put("dependencyType", dependency.getDependencyType());
            m.put("versionId", dependency.getVersionId());
            m.put("projectTitle", dependency.getProjectTitle() == null
                ? null
                : Utils.stringToBase64(dependency.getProjectTitle()));
            result.add(m);
        }
        return result;
    }

    private HashMap<String, Object> compatibilityMap() {
        HashMap<String, Object> m = new HashMap<>();
        m.put("serverType", server.getServerType().getName());
        m.put("mcVersion", mcVersion());
        m.put("loaders", ModrinthApi.modrinthLoaders(server.getServerType()));
        return m;
    }

    private static ServerType parsePlatform(String value) {
        if(value == null || value.isBlank()) return null;
        for(ServerType type : ServerType.values()) {
            if(type.getName().equalsIgnoreCase(value)) return type;
        }
        return null;
    }

    private static int parseInt(String value, int fallback) {
        if(value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}