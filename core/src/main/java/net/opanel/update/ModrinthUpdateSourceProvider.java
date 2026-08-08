package net.opanel.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.ServerType;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModrinthUpdateSourceProvider implements UpdateSourceProvider {
    private static final int MAX_HASHES_PER_REQUEST = 100;

    private final ModrinthApi api;
    private volatile String latestVersionBaseUrl;

    public ModrinthUpdateSourceProvider() {
        this(ModrinthApi.SOURCE_BOTH);
    }

    public ModrinthUpdateSourceProvider(String source) {
        this.api = new ModrinthApi(source);
    }

    /**
     * Switches which API endpoints this provider talks to: {@link ModrinthApi#SOURCE_MCIM}
     * (MCIM mirror only), {@link ModrinthApi#SOURCE_MODRINTH} (official Modrinth only) or
     * {@link ModrinthApi#SOURCE_BOTH} (MCIM first, falling back to official Modrinth).
     */
    public void setSource(String source) {
        api.setSource(source);
    }

    public static boolean isValidSource(String source) {
        return ModrinthApi.isValidSource(source);
    }

    @Override
    public String getSource() {
        return "modrinth";
    }

    @Override
    public boolean supportsAutomaticIdentification() {
        return true;
    }

    @Override
    public boolean isAutoApplySafe() {
        return true;
    }

    @Override
    public List<PluginUpdate> check(
        Path pluginsPath,
        List<OPanelPlugin> plugins,
        Map<String, String> fileHashes,
        String serverVersion,
        ServerType serverType,
        PluginUpdateConfig config
    ) throws IOException {
        return checkNow(fileHashes, plugins, serverVersion, serverType);
    }

    private List<PluginUpdate> checkNow(
        Map<String, String> fileHashes,
        List<OPanelPlugin> plugins,
        String serverVersion,
        ServerType serverType
    ) throws IOException {
        List<PluginUpdate> result = new ArrayList<>();
        if(fileHashes.isEmpty()) return result;

        final String mcVersion = ModrinthApi.extractMcVersion(serverVersion);
        List<JsonObject> installedMatches = matchVersionsByHashes(fileHashes.values());
        List<JsonObject> latestMatches = matchLatestVersionsByHashes(fileHashes.values(), mcVersion, serverType);

        Map<String, JsonObject> installedVersions = new LinkedHashMap<>();
        Map<String, JsonObject> latestVersions = new LinkedHashMap<>();
        Set<String> projectIds = new LinkedHashSet<>();
        int index = 0;
        for(String fileName : fileHashes.keySet()) {
            JsonObject installed = installedMatches.get(index);
            JsonObject latest = latestMatches.get(index++);
            if(installed == null || latest == null || !isPublishedAfter(installed, latest)) continue;
            if(!isAllowedChannel(
                getString(installed, "version_type", "release"),
                getString(latest, "version_type", "release")
            )) continue;

            installedVersions.put(fileName, installed);
            latestVersions.put(fileName, latest);
            projectIds.add(getString(installed, "project_id", ""));
        }
        if(installedVersions.isEmpty()) return result;

        projectIds.remove("");
        Map<String, JsonObject> projectsById = fetchProjects(projectIds);
        Map<String, OPanelPlugin> pluginsByName = new HashMap<>();
        for(OPanelPlugin plugin : plugins) pluginsByName.put(plugin.getFileName(), plugin);

        for(Map.Entry<String, JsonObject> entry : installedVersions.entrySet()) {
            final String fileName = entry.getKey();
            final JsonObject installed = entry.getValue();
            final JsonObject target = latestVersions.get(fileName);
            final String projectId = getString(installed, "project_id", "");

            JsonObject project = projectsById.get(projectId);
            String name = project != null && project.has("title") && !project.get("title").isJsonNull()
                ? project.get("title").getAsString()
                : getString(target, "name", fileName.replaceAll("\\.jar(\\"+ OPanelPlugin.DISABLED_SUFFIX +")?$", ""));
            if(name == null || name.isEmpty()) name = fileName;

            OPanelPlugin plugin = pluginsByName.get(fileName);
            final String currentVersion = getString(
                installed,
                "version_number",
                plugin == null || plugin.getVersion() == null ? "" : plugin.getVersion()
            );
            final String latestVersion = getString(target, "version_number", "");

            final String downloadUrl = getPrimaryFileUrl(target);
            if(downloadUrl == null) continue;
            final String downloadSha1 = getPrimaryFileSha1(target);
            if(downloadSha1 == null || downloadSha1.isEmpty()) {
                // Cannot safely auto-apply without a checksum; skip this plugin
                // rather than aborting detection for every other plugin.
                continue;
            }

            String projectUrl = null;
            if(project != null && project.has("slug") && project.has("project_type")) {
                projectUrl = "https://modrinth.com/"+ project.get("project_type").getAsString() +"/"+ project.get("slug").getAsString();
            }

            final String channel = target.has("version_type")
                ? target.get("version_type").getAsString()
                : "release";
            result.add(new PluginUpdate(
                fileName,
                name,
                currentVersion,
                latestVersion,
                downloadUrl,
                projectUrl,
                downloadSha1,
                fileHashes.get(fileName),
                getSource(),
                projectId,
                false,
                false,
                channel,
                "sha1",
                downloadSha1
            ));
        }
        return result;
    }

    private List<JsonObject> matchVersionsByHashes(Iterable<String> hashes) throws IOException {
        List<String> hashList = new ArrayList<>();
        for(String hash : hashes) hashList.add(hash);

        List<JsonObject> results = new ArrayList<>();
        for(int start = 0; start < hashList.size(); start += MAX_HASHES_PER_REQUEST) {
            int end = Math.min(start + MAX_HASHES_PER_REQUEST, hashList.size());
            List<String> batch = hashList.subList(start, end);
            results.addAll(mapVersions(batch, postVersionFiles(batch)));
        }
        return results;
    }

    private List<JsonObject> matchLatestVersionsByHashes(
        Iterable<String> hashes,
        String mcVersion,
        ServerType serverType
    ) throws IOException {
        List<String> hashList = new ArrayList<>();
        for(String hash : hashes) hashList.add(hash);

        List<JsonObject> results = new ArrayList<>();
        for(int start = 0; start < hashList.size(); start += MAX_HASHES_PER_REQUEST) {
            int end = Math.min(start + MAX_HASHES_PER_REQUEST, hashList.size());
            List<String> batch = hashList.subList(start, end);
            results.addAll(mapVersions(
                batch,
                postLatestVersionFiles(batch, mcVersion, serverType)
            ));
        }
        latestVersionBaseUrl = api.getLastResponseBaseUrl();
        return results;
    }

    private static JsonObject buildVersionFilesRequest(List<String> hashes) {
        JsonArray hashesBody = new JsonArray();
        for(String hash : hashes) hashesBody.add(hash);

        JsonObject body = new JsonObject();
        body.add("hashes", hashesBody);
        body.addProperty("algorithm", "sha1");
        return body;
    }

    private static JsonObject buildLatestVersionFilesRequest(
        List<String> hashes,
        String mcVersion,
        ServerType serverType
    ) {
        JsonObject body = buildVersionFilesRequest(hashes);
        JsonArray loaders = new JsonArray();
        for(String loader : ModrinthApi.modrinthLoaders(serverType)) loaders.add(loader);
        body.add("loaders", loaders);

        JsonArray gameVersions = new JsonArray();
        if(mcVersion != null && !mcVersion.isBlank()) gameVersions.add(mcVersion);
        body.add("game_versions", gameVersions);
        return body;
    }

    private static List<JsonObject> mapVersions(List<String> hashes, JsonObject response) {
        List<JsonObject> result = new ArrayList<>();
        for(String hash : hashes) {
            JsonElement version = response.get(hash);
            result.add(version == null || version.isJsonNull() ? null : version.getAsJsonObject());
        }
        return result;
    }

    private Map<String, JsonObject> fetchProjects(Collection<String> projectIds) {
        Map<String, JsonObject> result = new HashMap<>();
        if(projectIds.isEmpty()) return result;

        JsonArray body = new JsonArray();
        for(String projectId : projectIds) body.add(projectId);
        String encoded = URLEncoder.encode(body.toString(), StandardCharsets.UTF_8);
        try {
            JsonArray projects = api.getJson("/projects?ids="+ encoded).getAsJsonArray();
            for(JsonElement element : projects) {
                JsonObject project = element.getAsJsonObject();
                if(project.has("id") && !project.get("id").isJsonNull()) {
                    result.put(project.get("id").getAsString(), project);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Project metadata only affects the display name and project link.
            // Update detection can continue using the plugin file name.
        }
        return result;
    }

    private static boolean isAllowedChannel(String installedChannel, String candidateChannel) {
        if("release".equals(installedChannel)) return "release".equals(candidateChannel);
        if("beta".equals(installedChannel)) return !"alpha".equals(candidateChannel);
        return true;
    }

    private JsonObject getPrimaryFile(JsonObject version) {
        if(!version.has("files")) return null;
        JsonArray files = version.getAsJsonArray("files");
        if(files.size() == 0) return null;
        for(JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            if(file.has("primary") && file.get("primary").getAsBoolean()) return file;
        }
        return files.get(0).getAsJsonObject();
    }

    private String getPrimaryFileUrl(JsonObject version) {
        JsonObject file = getPrimaryFile(version);
        if(file != null && file.has("url") && !file.get("url").isJsonNull()) {
            String url = file.get("url").getAsString();
            // Only rewrite the download URL to the MCIM mirror when the version
            // response itself came from the mirror. When the official Modrinth
            // API served the response the CDN link is left untouched.
            return ModrinthApi.isMcimBaseUrl(latestVersionBaseUrl) ? ModrinthApi.rewriteCdnUrl(url) : url;
        }
        return null;
    }

    private String getPrimaryFileSha1(JsonObject version) {
        JsonObject file = getPrimaryFile(version);
        if(file == null || !file.has("hashes")) return null;
        JsonObject hashes = file.getAsJsonObject("hashes");
        if(hashes.has("sha1")) {
            return hashes.get("sha1").getAsString();
        }
        return null;
    }

    private JsonObject postVersionFiles(List<String> hashes) throws IOException {
        return api.postJson("/version_files", buildVersionFilesRequest(hashes));
    }

    private JsonObject postLatestVersionFiles(
        List<String> hashes,
        String mcVersion,
        ServerType serverType
    ) throws IOException {
        return api.postJson(
            "/version_files/update",
            buildLatestVersionFilesRequest(hashes, mcVersion, serverType)
        );
    }

    private static boolean isPublishedAfter(JsonObject installed, JsonObject target) {
        if(
            installed.has("id")
            && target.has("id")
            && installed.get("id").getAsString().equals(target.get("id").getAsString())
        ) {
            return false;
        }
        if(!installed.has("date_published") || !target.has("date_published")) return false;
        return target.get("date_published").getAsString()
            .compareTo(installed.get("date_published").getAsString()) > 0;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if(object != null && object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return fallback;
    }
}