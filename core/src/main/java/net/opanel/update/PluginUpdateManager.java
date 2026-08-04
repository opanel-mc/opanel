package net.opanel.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import net.opanel.exception.ActLaterException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginUpdateManager {
    private static final String API_BASE_URL = "https://api.modrinth.com/v2";
    private static final long CACHE_DURATION_MS = 12 * 60 * 60 * 1000L;
    private static final int MAX_HASHES_PER_REQUEST = 100;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");

    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final ExecutorService executor = Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "opanel-plugin-update-checker");
        thread.setDaemon(true);
        return thread;
    });

    private volatile long lastCheckedAt = 0;
    private volatile List<PluginUpdate> cachedUpdates = Collections.emptyList();
    private volatile Map<String, String> cachedFileHashes = Collections.emptyMap();
    private volatile String cachedServerVersion;
    private volatile ServerType cachedServerType;

    public PluginUpdateManager() {
        this(HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build(), API_BASE_URL);
    }

    PluginUpdateManager(HttpClient httpClient, String apiBaseUrl) {
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Returns the plugins / mods that have a newer version available on Modrinth.
     */
    public synchronized List<PluginUpdate> check(
        Path pluginsPath,
        List<OPanelPlugin> plugins,
        String serverVersion,
        ServerType serverType,
        boolean force
    ) throws IOException {
        final Map<String, String> fileHashes = calculateFileHashes(pluginsPath, plugins);
        if(
            !force
            && System.currentTimeMillis() - lastCheckedAt < CACHE_DURATION_MS
            && Objects.equals(cachedServerVersion, serverVersion)
            && cachedServerType == serverType
            && cachedFileHashes.equals(fileHashes)
        ) {
            return new ArrayList<>(cachedUpdates);
        }

        List<PluginUpdate> updates = checkNow(fileHashes, serverVersion, serverType);
        lastCheckedAt = System.currentTimeMillis();
        cachedUpdates = Collections.unmodifiableList(new ArrayList<>(updates));
        cachedFileHashes = Collections.unmodifiableMap(new LinkedHashMap<>(fileHashes));
        cachedServerVersion = serverVersion;
        cachedServerType = serverType;
        return new ArrayList<>(updates);
    }

    public synchronized void invalidateCache() {
        lastCheckedAt = 0;
        cachedUpdates = Collections.emptyList();
        cachedFileHashes = Collections.emptyMap();
        cachedServerVersion = null;
        cachedServerType = null;
    }

    /**
     * Downloads and replaces the given plugin / mod files. Each file is
     * downloaded to a temporary location first and the file integrity is
     * verified before any of the original files are replaced.
     */
    public synchronized void update(OPanelServer server, List<PluginUpdate> updates) throws IOException, ActLaterException {
        List<Path> tempFiles = new ArrayList<>();
        try {
            // Do not let a cached update overwrite a plugin that has changed since the check.
            for(PluginUpdate update : updates) {
                Path currentFile = resolvePluginFilePath(server.getPluginsPath(), update.getFileName());
                if(
                    currentFile == null
                    || !update.getInstalledFileSha1().equalsIgnoreCase(sha1(currentFile))
                ) {
                    throw new PluginUpdateConflictException(update.getFileName());
                }
            }

            for(PluginUpdate update : updates) {
                Path tempFile = OPanel.TMP_DIR_PATH.resolve("plugin-update-"+ java.util.UUID.randomUUID() +".jar");
                download(update.getDownloadUrl(), tempFile);

                final String expectedSha1 = update.getFileSha1();
                if(expectedSha1 != null && !expectedSha1.equalsIgnoreCase(sha1(tempFile))) {
                    throw new IOException("Downloaded file integrity check failed for "+ update.getName());
                }
                tempFiles.add(tempFile);
            }

            final boolean deferred = applyDownloadedUpdates(updates, tempFiles, server::updatePlugin);
            if(deferred) {
                throw new ActLaterException();
            }
        } finally {
            invalidateCache();
            for(Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    //
                }
            }
        }
    }

    static boolean applyDownloadedUpdates(
        List<PluginUpdate> updates,
        List<Path> tempFiles,
        PluginFileUpdater updater
    ) throws IOException {
        boolean deferred = false;
        for(int i = 0; i < updates.size(); i++) {
            try {
                updater.update(updates.get(i).getFileName(), tempFiles.get(i));
            } catch (ActLaterException e) {
                deferred = true;
            }
        }
        return deferred;
    }

    @FunctionalInterface
    interface PluginFileUpdater {
        void update(String fileName, Path newPluginFile) throws IOException, ActLaterException;
    }

    private Map<String, String> calculateFileHashes(Path pluginsPath, List<OPanelPlugin> plugins) {
        Map<String, String> fileHashes = new LinkedHashMap<>();
        for(OPanelPlugin plugin : plugins) {
            Path filePath = resolvePluginFilePath(pluginsPath, plugin.getFileName());
            if(filePath == null) continue;
            try {
                fileHashes.put(plugin.getFileName(), sha1(filePath));
            } catch (IOException e) {
                // Skip files that cannot be read.
            }
        }
        return fileHashes;
    }

    private List<PluginUpdate> checkNow(
        Map<String, String> fileHashes,
        String serverVersion,
        ServerType serverType
    ) throws IOException {
        List<PluginUpdate> result = new ArrayList<>();
        if(fileHashes.isEmpty()) return result;

        // Match the hashes against the Modrinth version database
        List<JsonObject> matchedVersions = matchVersionsByHashes(fileHashes.values());
        if(matchedVersions.isEmpty()) return result;

        Map<String, JsonObject> installedVersions = new LinkedHashMap<>();
        Set<String> projectIds = new LinkedHashSet<>();
        int index = 0;
        for(String fileName : fileHashes.keySet()) {
            JsonObject version = matchedVersions.get(index++);
            if(version == null) continue;
            installedVersions.put(fileName, version);
            projectIds.add(version.get("project_id").getAsString());
        }
        if(installedVersions.isEmpty()) return result;

        Map<String, JsonObject> projects = fetchProjects(projectIds);
        Map<String, JsonArray> versionLists = fetchVersionLists(projectIds);

        final String mcVersion = extractMcVersion(serverVersion);
        final String mcPatchless = mcVersion != null && mcVersion.split("\\.").length == 3
            ? mcVersion.substring(0, mcVersion.lastIndexOf("."))
            : null;

        for(Map.Entry<String, JsonObject> entry : installedVersions.entrySet()) {
            final String fileName = entry.getKey();
            final JsonObject installed = entry.getValue();
            final String projectId = installed.get("project_id").getAsString();

            JsonObject project = projects.get(projectId);
            String name = project != null && project.has("title") && !project.get("title").isJsonNull()
                ? project.get("title").getAsString()
                : fileName.replaceAll("\\.jar(\\"+ OPanelPlugin.DISABLED_SUFFIX +")?$", "");
            if(name == null || name.isEmpty()) name = fileName;

            JsonObject target = pickTarget(versionLists.get(projectId), mcVersion, mcPatchless, serverType, installed);
            if(target == null) continue;

            final String currentVersion = installed.get("version_number").getAsString();
            final String latestVersion = target.get("version_number").getAsString();
            if(!isPublishedAfter(installed, target)) continue;

            final String downloadUrl = getPrimaryFileUrl(target);
            if(downloadUrl == null) continue;
            final String downloadSha1 = getPrimaryFileSha1(target);
            if(downloadSha1 == null || downloadSha1.isEmpty()) {
                throw new IOException("Modrinth did not provide a SHA-1 hash for " + name);
            }

            String projectUrl = null;
            if(project != null && project.has("slug") && project.has("project_type")) {
                projectUrl = "https://modrinth.com/"+ project.get("project_type").getAsString() +"/"+ project.get("slug").getAsString();
            }

            result.add(new PluginUpdate(
                fileName,
                name,
                currentVersion,
                latestVersion,
                downloadUrl,
                projectUrl,
                downloadSha1,
                fileHashes.get(fileName)
            ));
        }
        return result;
    }

    List<JsonObject> matchVersionsByHashes(Iterable<String> hashes) throws IOException {
        List<String> hashList = new ArrayList<>();
        for(String hash : hashes) hashList.add(hash);

        List<JsonObject> results = new ArrayList<>();
        for(int start = 0; start < hashList.size(); start += MAX_HASHES_PER_REQUEST) {
            int end = Math.min(start + MAX_HASHES_PER_REQUEST, hashList.size());
            JsonObject body = buildVersionFilesRequest(hashList.subList(start, end));
            JsonObject response = postJson(apiBaseUrl +"/version_files", body).getAsJsonObject();
            results.addAll(mapVersionsByHash(hashList.subList(start, end), response));
        }
        return results;
    }

    static JsonObject buildVersionFilesRequest(List<String> hashes) {
        JsonArray hashesBody = new JsonArray();
        for(String hash : hashes) hashesBody.add(hash);

        JsonObject body = new JsonObject();
        body.add("hashes", hashesBody);
        body.addProperty("algorithm", "sha1");
        return body;
    }

    static List<JsonObject> mapVersionsByHash(List<String> hashes, JsonObject response) {
        List<JsonObject> result = new ArrayList<>();
        for(String hash : hashes) {
            JsonElement version = response.get(hash);
            result.add(version == null || version.isJsonNull() ? null : version.getAsJsonObject());
        }
        return result;
    }

    private Map<String, JsonObject> fetchProjects(Collection<String> projectIds) throws IOException {
        Map<String, JsonObject> result = new HashMap<>();
        if(projectIds.isEmpty()) return result;

        JsonArray body = new JsonArray();
        for(String projectId : projectIds) body.add(projectId);
        String encoded = URLEncoder.encode(body.toString(), StandardCharsets.UTF_8);
        JsonArray projects = getJson(apiBaseUrl +"/projects?ids="+ encoded).getAsJsonArray();
        for(JsonElement element : projects) {
            JsonObject project = element.getAsJsonObject();
            result.put(project.get("id").getAsString(), project);
        }
        return result;
    }

    private Map<String, JsonArray> fetchVersionLists(Collection<String> projectIds) throws IOException {
        Map<String, JsonArray> result = new ConcurrentHashMap<>();
        Queue<IOException> errors = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for(String projectId : projectIds) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    JsonElement response = getJson(apiBaseUrl +"/project/"+ projectId +"/version");
                    if(response.isJsonArray()) {
                        result.put(projectId, response.getAsJsonArray());
                    }
                } catch (IOException e) {
                    errors.add(e);
                }
            }, executor));
        }
        for(CompletableFuture<Void> future : futures) {
            future.join();
        }
        if(!errors.isEmpty()) {
            throw errors.remove();
        }
        return result;
    }

    static JsonObject pickTarget(
        JsonArray versions,
        String mcVersion,
        String mcPatchless,
        ServerType serverType,
        JsonObject installedVersion
    ) {
        if(versions == null) return null;

        List<JsonObject> list = new ArrayList<>();
        for(JsonElement element : versions) {
            list.add(element.getAsJsonObject());
        }
        // The API returns versions ordered newest first, but sort defensively anyway
        list.sort((a, b) -> {
            String dateA = a.has("date_published") ? a.get("date_published").getAsString() : "";
            String dateB = b.has("date_published") ? b.get("date_published").getAsString() : "";
            return dateB.compareTo(dateA);
        });

        final String installedChannel = installedVersion.has("version_type")
            ? installedVersion.get("version_type").getAsString()
            : "release";
        for(JsonObject version : list) {
            String type = version.has("version_type") ? version.get("version_type").getAsString() : "release";
            if(!isAllowedChannel(installedChannel, type)) continue;
            if(!matchesGameVersion(version, mcVersion, mcPatchless)) continue;
            if(!matchesLoader(version, serverType)) continue;
            return version;
        }
        return null;
    }

    private static boolean isAllowedChannel(String installedChannel, String candidateChannel) {
        if("release".equals(installedChannel)) return "release".equals(candidateChannel);
        if("beta".equals(installedChannel)) return !"alpha".equals(candidateChannel);
        return true;
    }

    static boolean matchesLoader(JsonObject version, ServerType serverType) {
        if(!version.has("loaders") || !version.get("loaders").isJsonArray()) return false;

        final Set<String> allowedLoaders;
        switch(serverType) {
            case FABRIC:
                allowedLoaders = Set.of("fabric");
                break;
            case FORGE:
                allowedLoaders = Set.of("forge");
                break;
            case NEOFORGE:
                allowedLoaders = Set.of("neoforge");
                break;
            case FOLIA:
                allowedLoaders = Set.of("folia");
                break;
            case PAPER:
            case LEAVES:
                allowedLoaders = Set.of("paper", "purpur", "spigot", "bukkit");
                break;
            default:
                return false;
        }

        for(JsonElement element : version.getAsJsonArray("loaders")) {
            if(allowedLoaders.contains(element.getAsString().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static boolean matchesGameVersion(JsonObject version, String mcVersion, String mcPatchless) {
        if(mcVersion == null || !version.has("game_versions")) return true;
        for(JsonElement element : version.getAsJsonArray("game_versions")) {
            String gameVersion = element.getAsString();
            if(gameVersion.equals(mcVersion) || (mcPatchless != null && gameVersion.equals(mcPatchless))) return true;
        }
        return false;
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
            return file.get("url").getAsString();
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

    private JsonElement getJson(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (github.com/opanel-mc/opanel)")
            .GET()
            .build();
        return send(request);
    }

    private JsonElement postJson(String url, JsonElement body) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (github.com/opanel-mc/opanel)")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        return send(request);
    }

    private JsonElement send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) {
                throw new IOException("Modrinth API returned HTTP " + response.statusCode());
            }
            return JsonParser.parseString(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting the Modrinth API", e);
        }
    }

    private void download(String url, Path target) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(10))
            .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (https://github.com/opanel-mc/opanel)")
            .GET()
            .build();
        try {
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if(response.statusCode() != 200) {
                Files.deleteIfExists(target);
                throw new IOException("Failed to download the update file (HTTP " + response.statusCode() +")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Files.deleteIfExists(target);
            throw new IOException("Interrupted while downloading the update file", e);
        }
    }

    static Path resolvePluginFilePath(Path pluginsPath, String fileName) {
        Path path = pluginsPath.resolve(fileName);
        if(Files.exists(path)) return path;
        Path disabledPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        return Files.exists(disabledPath) ? disabledPath : null;
    }

    public static String sha1(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try(InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder(40);
            for(byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm is not available", e);
        }
    }

    public static String extractMcVersion(String serverVersion) {
        if(serverVersion == null || serverVersion.isEmpty()) return null;
        Matcher matcher = MC_VERSION_PATTERN.matcher(serverVersion);
        return matcher.find() ? matcher.group() : null;
    }

    static boolean isPublishedAfter(JsonObject installed, JsonObject target) {
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
}
