package net.opanel.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import net.opanel.exception.MarketplaceInstallConflictException;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Browse and install Modrinth projects (plugins / mods) from inside the panel.
 *
 * <p>Search and version resolution reuse {@link ModrinthApi} so the mirror
 * preference configured for update detection applies to the marketplace too.
 * Installations download the primary jar into {@code OPanelServer#getPluginsPath()}
 * after verifying its SHA-1, so they take effect on the next server restart.</p>
 */
public class MarketplaceService {
    private static final int MAX_HASHES_PER_REQUEST = 100;
    private static final int MAX_MISSING_DEPENDENCIES = 10;
    private static final int MAX_DEPENDENCY_DEPTH = 3;
    private static final int MAX_VERSIONS = 20;
    private static final long INSTALLED_CACHE_MS = 60_000L;
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);

    private final OPanel plugin;
    private final OPanelServer server;
    private final ModrinthApi api;

    private volatile long installedCacheAt;
    private volatile Set<String> installedProjectIds = Collections.emptySet();

    public MarketplaceService(OPanel plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.api = new ModrinthApi(plugin.getConfig().modrinthApiSource);
    }

    /** Applies the currently configured source ({@code mcim} / {@code modrinth} / {@code both}). */
    public void setSource(String source) {
        api.setSource(source);
    }

    public SearchResult search(
        String query,
        String category,
        ServerType platform,
        String gameVersion,
        boolean compatibleOnly,
        String sort,
        int offset,
        int limit
    ) throws IOException {
        final ServerType effectiveType = platform == null ? server.getServerType() : platform;
        final String effectiveGameVersion = (gameVersion == null || gameVersion.isBlank())
            ? ModrinthApi.extractMcVersion(server.getVersion())
            : gameVersion;

        final List<String> loaders = ModrinthApi.modrinthLoaders(effectiveType);

        final JsonArray facets = new JsonArray();
        if(!loaders.isEmpty()) {
            final JsonArray loaderFacet = new JsonArray();
            for(String loader : loaders) loaderFacet.add("categories:"+ loader);
            facets.add(loaderFacet);
        }
        if(category != null && !category.isBlank()) {
            final JsonArray categoryFacet = new JsonArray();
            categoryFacet.add("categories:"+ category);
            facets.add(categoryFacet);
        }
        if(compatibleOnly && effectiveGameVersion != null && !effectiveGameVersion.isBlank()) {
            final JsonArray versionFacet = new JsonArray();
            versionFacet.add("versions:"+ effectiveGameVersion);
            facets.add(versionFacet);
        }

        final StringBuilder url = new StringBuilder("/search")
            .append("?query=").append(URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8))
            .append("&limit=").append(Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100)))
            .append("&offset=").append(Math.max(0, offset));
        if(sort != null && !sort.isBlank()) {
            url.append("&index=").append(URLEncoder.encode(sort, StandardCharsets.UTF_8));
        }
        if(facets.size() > 0) {
            url.append("&facets=").append(URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8));
        }

        final ModrinthApi.ApiResponse<JsonElement> apiResponse = api.getJsonResponse(url.toString());
        final JsonObject response = apiResponse.getBody().getAsJsonObject();
        final List<ProjectHit> hits = new ArrayList<>();
        final JsonArray hitsArray = response.has("hits") && response.get("hits").isJsonArray()
            ? response.getAsJsonArray("hits")
            : new JsonArray();
        for(JsonElement element : hitsArray) {
            if(!element.isJsonObject()) continue;
            final JsonObject hit = element.getAsJsonObject();
            hits.add(new ProjectHit(
                getString(hit, "project_id", ""),
                getString(hit, "slug", ""),
                getString(hit, "title", ""),
                getString(hit, "author", ""),
                getString(hit, "description", ""),
                rewriteIcon(getString(hit, "icon_url", null), apiResponse.isMcimBased()),
                getLong(hit, "downloads", 0),
                getLong(hit, "follows", 0),
                buildProjectUrl(hit),
                getString(hit, "project_type", ""),
                getStringList(hit, "display_categories"),
                getString(hit, "date_modified", getString(hit, "date_created", ""))
            ));
        }
        return new SearchResult(hits, getInt(response, "total_hits", hits.size()), offset, limit);
    }

    public ProjectDetail getProject(String projectId) throws IOException {
        final ModrinthApi.ApiResponse<JsonElement> projectResponse = api.getJsonResponse("/project/"+ URLEncoder.encode(projectId, StandardCharsets.UTF_8));
        final JsonObject project = projectResponse.getBody().getAsJsonObject();

        final ServerType type = server.getServerType();
        final List<String> loaders = ModrinthApi.modrinthLoaders(type);
        final String mc = ModrinthApi.extractMcVersion(server.getVersion());
        VersionListResponse versionsResponse = fetchVersions(projectId, loaders, mc);
        JsonArray rawVersions = versionsResponse.versions;
        boolean versionsFilteredByGame = true;
        if(rawVersions.isEmpty() && mc != null) {
            versionsResponse = fetchVersions(projectId, loaders, null);
            rawVersions = versionsResponse.versions;
            versionsFilteredByGame = false;
        }

        final List<MarketplaceVersion> versions = new ArrayList<>();
        for(JsonElement element : rawVersions) {
            if(!element.isJsonObject()) continue;
            final MarketplaceVersion version = toMarketplaceVersion(element.getAsJsonObject(), loaders, mc, versionsResponse.mcimBased);
            if(version != null) versions.add(version);
        }
        versions.sort(Comparator.comparing(MarketplaceVersion::getDatePublished, Comparator.nullsLast(Comparator.reverseOrder())));
        final List<MarketplaceVersion> capped = versions.size() > MAX_VERSIONS
            ? new ArrayList<>(versions.subList(0, MAX_VERSIONS))
            : versions;

        return new ProjectDetail(
            getString(project, "id", projectId),
            getString(project, "slug", ""),
            getString(project, "title", ""),
            getString(project, "author", ""),
            getString(project, "description", ""),
            rewriteIcon(getString(project, "icon_url", null), projectResponse.isMcimBased()),
            buildProjectUrl(project),
            getString(project, "source_url", null),
            getString(project, "project_type", ""),
            getString(project, "updated", getString(project, "created", "")),
            getLong(project, "downloads", 0),
            getLong(project, "followers", 0),
            getStringList(project, "categories"),
            getStringList(project, "versions"),
            capped,
            versionsFilteredByGame
        );
    }

    public InstallPreview previewInstall(String targetProjectId, String versionId) throws IOException {
        if(targetProjectId == null || versionId == null || targetProjectId.isBlank() || versionId.isBlank()) {
            throw new IOException("Project id and version id are required.");
        }

        final Set<String> installed = resolveInstalledProjectIds();
        final VersionResponse targetVersion = fetchVersion(targetProjectId, versionId);
        final SelectedFile target = toSelectedFile(targetProjectId, fetchProjectTitle(targetProjectId), versionId, targetVersion.version, targetVersion.mcimBased);

        final List<SelectedFile> missing = new ArrayList<>();
        final List<DependencyInfo> alreadyInstalled = new ArrayList<>();
        final List<DependencyInfo> unresolved = new ArrayList<>();
        final Set<String> visited = new LinkedHashSet<>();
        visited.add(targetProjectId);

        final ArrayDeque<DependencyRef> queue = new ArrayDeque<>();
        enqueueRequiredDependencies(targetVersion.version, queue, 1);
        while(!queue.isEmpty()) {
            if(missing.size() + unresolved.size() >= MAX_MISSING_DEPENDENCIES) break;
            final DependencyRef dep = queue.poll();
            if(dep == null || !visited.add(dep.projectId)) continue;

            final String title = fetchProjectTitle(dep.projectId);
            if(installed.contains(dep.projectId)) {
                alreadyInstalled.add(new DependencyInfo(dep.projectId, "required", dep.versionId, title));
                continue;
            }

            final VersionResponse version = resolveDependencyVersion(dep.projectId, dep.versionId);
            if(version == null) {
                unresolved.add(new DependencyInfo(dep.projectId, "required", dep.versionId, title));
                continue;
            }

            missing.add(toSelectedFile(dep.projectId, title, getString(version.version, "id", dep.versionId), version.version, version.mcimBased));
            if(dep.depth < MAX_DEPENDENCY_DEPTH) {
                enqueueRequiredDependencies(version.version, queue, dep.depth + 1);
            }
        }
        return new InstallPreview(target, missing, alreadyInstalled, unresolved);
    }

    public synchronized InstallResult install(List<InstallEntry> entries) throws IOException, MarketplaceInstallConflictException {
        final List<Path> tempFiles = new ArrayList<>();
        final List<Path> movedFiles = new ArrayList<>();
        boolean complete = false;
        try {
            final List<SelectedFile> installed = new ArrayList<>();
            final List<PendingInstall> pending = new ArrayList<>();
            final Set<String> targetNames = new LinkedHashSet<>();
            final Path pluginsPath = server.getPluginsPath();
            for(InstallEntry entry : entries) {
                if(entry == null || entry.projectId == null || entry.versionId == null
                    || entry.projectId.isBlank() || entry.versionId.isBlank()) {
                    throw new IllegalArgumentException("Project id and version id are required.");
                }
                final VersionResponse version = fetchVersion(entry.projectId, entry.versionId);
                final SelectedFile file = toSelectedFile(
                    entry.projectId,
                    fetchProjectTitle(entry.projectId),
                    entry.versionId,
                    version.version,
                    version.mcimBased
                );
                if(!Utils.isSafeFileName(file.fileName)) {
                    throw new IllegalArgumentException("Illegal file name.");
                }
                if(!file.fileName.endsWith(".jar")) {
                    throw new IllegalArgumentException("The file should be a .jar file.");
                }

                final Path target = pluginsPath.resolve(file.fileName);
                final Path disabled = pluginsPath.resolve(file.fileName + OPanelPlugin.DISABLED_SUFFIX);
                if(!targetNames.add(file.fileName) || Files.exists(target) || Files.exists(disabled)) {
                    throw new MarketplaceInstallConflictException(file.fileName);
                }

                final Path temp = OPanel.TMP_DIR_PATH.resolve("marketplace-"+ UUID.randomUUID() +".jar");
                download(file.url, temp);
                tempFiles.add(temp);
                if(file.sha1 != null && !file.sha1.isEmpty() && !file.sha1.equalsIgnoreCase(Utils.sha1(temp))) {
                    throw new IOException("Downloaded file integrity check failed for "+ file.projectTitle);
                }
                pending.add(new PendingInstall(file, temp, target, disabled));
            }
            for(PendingInstall install : pending) {
                if(Files.exists(install.target) || Files.exists(install.disabled)) {
                    throw new MarketplaceInstallConflictException(install.file.fileName);
                }
            }
            for(PendingInstall install : pending) {
                try {
                    Files.move(install.temp, install.target);
                } catch (FileAlreadyExistsException e) {
                    // A file it created between the conflict re-check and this move
                    // (for example a concurrent install outside our lock). Surface it
                    // as a conflict; the finally block rolls back files moved so far.
                    throw new MarketplaceInstallConflictException(install.file.fileName);
                }
                movedFiles.add(install.target);
                installed.add(install.file);
            }
            complete = true;
            return new InstallResult(installed, true);
        } finally {
            if(!complete) {
                for(Path moved : movedFiles) {
                    try {
                        Files.deleteIfExists(moved);
                    } catch (IOException ignored) {
                        // Keep the original install failure as the reported cause.
                    }
                }
            }
            // The installed file becomes part of the next update check.
            plugin.getPluginUpdateManager().invalidateCache();
            installedCacheAt = 0;
            installedProjectIds = Collections.emptySet();
            for(Path temp : tempFiles) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    //
                }
            }
        }
    }

    private MarketplaceVersion toMarketplaceVersion(JsonObject version, List<String> loaders, String mc, boolean mcimBased) {
        final JsonObject primary = getPrimaryFile(version);
        if(primary == null) return null;
        final String fileName = getString(primary, "filename", null);
        if(fileName == null || !fileName.endsWith(".jar")) return null;
        String url = getString(primary, "url", null);
        if(url == null) return null;
        url = mcimBased ? ModrinthApi.rewriteCdnUrl(url) : url;

        final List<String> gameVersions = getStringList(version, "game_versions");
        final List<String> versionLoaders = getStringList(version, "loaders");
        return new MarketplaceVersion(
            getString(version, "id", ""),
            getString(version, "name", ""),
            getString(version, "version_number", ""),
            getString(version, "version_type", "release"),
            getString(version, "date_published", ""),
            gameVersions,
            versionLoaders,
            getLong(version, "downloads", 0),
            fileName,
            getLong(primary, "size", 0),
            url,
            getSha1(primary),
            isCompatible(gameVersions, versionLoaders, loaders, mc),
            toDependencies(version)
        );
    }

    private static boolean isCompatible(
        List<String> gameVersions,
        List<String> versionLoaders,
        List<String> serverLoaders,
        String mc
    ) {
        if(mc != null && !gameVersions.isEmpty() && !contains(gameVersions, mc)) return false;
        if(versionLoaders.isEmpty()) return !gameVersions.isEmpty();
        for(String loader : versionLoaders) {
            if(contains(serverLoaders, loader)) return true;
        }
        return false;
    }

    private VersionListResponse fetchVersions(String projectId, List<String> loaders, String gameVersion) throws IOException {
        final StringBuilder url = new StringBuilder("/project/").append(URLEncoder.encode(projectId, StandardCharsets.UTF_8)).append("/version");
        if(!loaders.isEmpty()) {
            final JsonArray loadersBody = new JsonArray();
            for(String loader : loaders) loadersBody.add(loader);
            url.append("?loaders=").append(URLEncoder.encode(loadersBody.toString(), StandardCharsets.UTF_8));
        }
        if(gameVersion != null && !gameVersion.isBlank()) {
            final JsonArray gameVersionsBody = new JsonArray();
            gameVersionsBody.add(gameVersion);
            url.append(loaders.isEmpty() ? "?" : "&")
                .append("game_versions=").append(URLEncoder.encode(gameVersionsBody.toString(), StandardCharsets.UTF_8));
        }
        final ModrinthApi.ApiResponse<JsonElement> response = api.getJsonResponse(url.toString());
        return new VersionListResponse(
            response.getBody().isJsonArray() ? response.getBody().getAsJsonArray() : new JsonArray(),
            response.isMcimBased()
        );
    }

    private VersionResponse fetchVersion(String projectId, String versionId) throws IOException {
        final ModrinthApi.ApiResponse<JsonElement> response = api.getJsonResponse("/project/"+ URLEncoder.encode(projectId, StandardCharsets.UTF_8) +"/version/"+ URLEncoder.encode(versionId, StandardCharsets.UTF_8));
        if(!response.getBody().isJsonObject()) throw new IOException("Cannot find the requested version.");
        return new VersionResponse(response.getBody().getAsJsonObject(), response.isMcimBased());
    }

    private String fetchProjectTitle(String projectId) {
        try {
            final JsonElement response = api.getJson("/project/"+ URLEncoder.encode(projectId, StandardCharsets.UTF_8));
            if(response.isJsonObject()) {
                final String title = getString(response.getAsJsonObject(), "title", null);
                if(title != null && !title.isBlank()) return title;
            }
        } catch (IOException ignored) {
            //
        }
        return projectId;
    }

    private VersionResponse resolveDependencyVersion(String projectId, String versionId) throws IOException {
        final List<String> loaders = ModrinthApi.modrinthLoaders(server.getServerType());
        final String mc = ModrinthApi.extractMcVersion(server.getVersion());
        if(versionId != null && !versionId.isBlank()) {
            final VersionResponse version = fetchVersion(projectId, versionId);
            if(isCompatible(getStringList(version.version, "game_versions"), getStringList(version.version, "loaders"), loaders, mc)) return version;
        }
        VersionListResponse versionResponse = fetchVersions(projectId, loaders, mc);
        JsonArray versions = versionResponse.versions;
        if(versions.isEmpty() && mc != null) {
            versionResponse = fetchVersions(projectId, loaders, null);
            versions = versionResponse.versions;
        }
        JsonObject best = null;
        String bestDate = "";
        for(JsonElement element : versions) {
            if(!element.isJsonObject()) continue;
            final JsonObject version = element.getAsJsonObject();
            if(!isCompatible(getStringList(version, "game_versions"), getStringList(version, "loaders"), loaders, mc)) continue;
            final String date = getString(version, "date_published", "");
            if(best == null || date.compareTo(bestDate) > 0) {
                best = version;
                bestDate = date;
            }
        }
        return best == null ? null : new VersionResponse(best, versionResponse.mcimBased);
    }

    private static void enqueueRequiredDependencies(JsonObject version, ArrayDeque<DependencyRef> queue, int depth) {
        if(version == null || !version.has("dependencies") || !version.get("dependencies").isJsonArray()) return;
        for(JsonElement element : version.getAsJsonArray("dependencies")) {
            if(!element.isJsonObject()) continue;
            final JsonObject dependency = element.getAsJsonObject();
            if(!"required".equals(getString(dependency, "dependency_type", ""))) continue;
            final String projectId = getString(dependency, "project_id", null);
            if(projectId == null || projectId.isBlank()) continue;
            queue.add(new DependencyRef(projectId, getString(dependency, "version_id", null), depth));
        }
    }

    private SelectedFile toSelectedFile(String projectId, String projectTitle, String versionId, JsonObject version, boolean mcimBased)
        throws IOException {
        final JsonObject primary = getPrimaryFile(version);
        if(primary == null) throw new IOException("The version has no downloadable file.");
        final String fileName = getString(primary, "filename", null);
        if(fileName == null || !fileName.endsWith(".jar")) throw new IOException("The primary file is not a .jar file.");
        String url = getString(primary, "url", null);
        if(url == null) throw new IOException("The version has no download URL.");
        url = mcimBased ? ModrinthApi.rewriteCdnUrl(url) : url;
        return new SelectedFile(
            projectId,
            projectTitle,
            versionId,
            getString(version, "version_number", ""),
            fileName,
            getLong(primary, "size", 0),
            url,
            getSha1(primary)
        );
    }

    private Set<String> resolveInstalledProjectIds() throws IOException {
        final long now = System.currentTimeMillis();
        if(now - installedCacheAt < INSTALLED_CACHE_MS) return installedProjectIds;

        final List<String> hashes = new ArrayList<>();
        final Path pluginsPath = server.getPluginsPath();
        for(OPanelPlugin plugin : server.getPlugins()) {
            final Path file = resolvePluginFilePath(pluginsPath, plugin.getFileName());
            if(file == null) continue;
            try {
                hashes.add(Utils.sha1(file));
            } catch (IOException ignored) {
                // Skip files that cannot be read.
            }
        }

        final Set<String> projectIds = new LinkedHashSet<>();
        for(int start = 0; start < hashes.size(); start += MAX_HASHES_PER_REQUEST) {
            final int end = Math.min(start + MAX_HASHES_PER_REQUEST, hashes.size());
            final List<String> batch = hashes.subList(start, end);
            final JsonObject body = new JsonObject();
            final JsonArray hashesBody = new JsonArray();
            for(String hash : batch) hashesBody.add(hash);
            body.add("hashes", hashesBody);
            body.addProperty("algorithm", "sha1");
            final JsonObject response = api.postJson("/version_files", body);
            for(String hash : batch) {
                final JsonElement version = response.get(hash);
                if(version == null || !version.isJsonObject()) continue;
                final String projectId = getString(version.getAsJsonObject(), "project_id", null);
                if(projectId != null && !projectId.isBlank()) projectIds.add(projectId);
            }
        }
        installedCacheAt = now;
        installedProjectIds = projectIds;
        return projectIds;
    }

    private static Path resolvePluginFilePath(Path pluginsPath, String fileName) {
        final Path path = pluginsPath.resolve(fileName);
        if(Files.exists(path)) return path;
        final Path disabledPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        return Files.exists(disabledPath) ? disabledPath : null;
    }

    private JsonObject getPrimaryFile(JsonObject version) {
        if(!version.has("files") || !version.get("files").isJsonArray()) return null;
        final JsonArray files = version.getAsJsonArray("files");
        if(files.size() == 0) return null;
        for(JsonElement element : files) {
            final JsonObject file = element.getAsJsonObject();
            if(file.has("primary") && file.get("primary").getAsBoolean()) return file;
        }
        return files.get(0).getAsJsonObject();
    }

    private String rewriteIcon(String iconUrl, boolean mcimBased) {
        if(iconUrl == null || iconUrl.isEmpty()) return null;
        return mcimBased ? ModrinthApi.rewriteCdnUrl(iconUrl) : iconUrl;
    }

    private static String getSha1(JsonObject file) {
        if(file == null || !file.has("hashes") || !file.get("hashes").isJsonObject()) return null;
        return getString(file.getAsJsonObject("hashes"), "sha1", null);
    }

    private static String buildProjectUrl(JsonObject object) {
        final String type = getString(object, "project_type", "mod");
        final String slug = getString(object, "slug", null);
        if(slug == null || slug.isBlank()) return "https://modrinth.com/";
        return "https://modrinth.com/"+ type +"/"+ slug;
    }

    private static List<DependencyInfo> toDependencies(JsonObject version) {
        final List<DependencyInfo> result = new ArrayList<>();
        if(version == null || !version.has("dependencies") || !version.get("dependencies").isJsonArray()) return result;
        for(JsonElement element : version.getAsJsonArray("dependencies")) {
            if(!element.isJsonObject()) continue;
            final JsonObject dependency = element.getAsJsonObject();
            final String projectId = getString(dependency, "project_id", null);
            if(projectId == null || projectId.isBlank()) continue;
            result.add(new DependencyInfo(
                projectId,
                getString(dependency, "dependency_type", ""),
                getString(dependency, "version_id", null),
                null
            ));
        }
        return result;
    }

    private static void download(String url, Path target) throws IOException {
        final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(DOWNLOAD_TIMEOUT)
            .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (github.com/opanel-mc/opanel)")
            .GET()
            .build();
        try {
            final HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if(response.statusCode() != 200) {
                Files.deleteIfExists(target);
                throw new IOException("Failed to download the file (HTTP "+ response.statusCode() +")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Files.deleteIfExists(target);
            throw new IOException("Interrupted while downloading the file", e);
        }
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if(object != null && object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return fallback;
    }

    private static long getLong(JsonObject object, String key, long fallback) {
        if(object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsLong();
            } catch (NumberFormatException ignored) {
                //
            }
        }
        return fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if(object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsInt();
            } catch (NumberFormatException ignored) {
                //
            }
        }
        return fallback;
    }

    private static List<String> getStringList(JsonObject object, String key) {
        final List<String> result = new ArrayList<>();
        if(object != null && object.has(key) && object.get(key).isJsonArray()) {
            for(JsonElement element : object.getAsJsonArray(key)) {
                if(element.isJsonPrimitive()) result.add(element.getAsString());
            }
        }
        return result;
    }

    private static boolean contains(List<String> list, String value) {
        if(list == null || value == null) return false;
        for(String item : list) {
            if(item.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    // ===== Value objects passed back to the controller =====

    public static class ProjectHit {
        private final String id;
        private final String slug;
        private final String title;
        private final String author;
        private final String summary;
        private final String iconUrl;
        private final long downloads;
        private final long follows;
        private final String projectUrl;
        private final String projectType;
        private final List<String> categories;
        private final String updatedAt;

        public ProjectHit(
            String id, String slug, String title, String author, String summary, String iconUrl,
            long downloads, long follows, String projectUrl, String projectType,
            List<String> categories, String updatedAt
        ) {
            this.id = id;
            this.slug = slug;
            this.title = title;
            this.author = author;
            this.summary = summary;
            this.iconUrl = iconUrl;
            this.downloads = downloads;
            this.follows = follows;
            this.projectUrl = projectUrl;
            this.projectType = projectType;
            this.categories = categories;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public String getSlug() { return slug; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getSummary() { return summary; }
        public String getIconUrl() { return iconUrl; }
        public long getDownloads() { return downloads; }
        public long getFollows() { return follows; }
        public String getProjectUrl() { return projectUrl; }
        public String getProjectType() { return projectType; }
        public List<String> getCategories() { return categories; }
        public String getUpdatedAt() { return updatedAt; }
    }

    public static class SearchResult {
        private final List<ProjectHit> hits;
        private final int totalHits;
        private final int offset;
        private final int limit;

        public SearchResult(List<ProjectHit> hits, int totalHits, int offset, int limit) {
            this.hits = hits;
            this.totalHits = totalHits;
            this.offset = offset;
            this.limit = limit;
        }

        public List<ProjectHit> getHits() { return hits; }
        public int getTotalHits() { return totalHits; }
        public int getOffset() { return offset; }
        public int getLimit() { return limit; }
    }

    public static class MarketplaceVersion {
        private final String id;
        private final String name;
        private final String versionNumber;
        private final String channel;
        private final String datePublished;
        private final List<String> gameVersions;
        private final List<String> loaders;
        private final long downloads;
        private final String fileName;
        private final long fileSize;
        private final String downloadUrl;
        private final String sha1;
        private final boolean compatible;
        private final List<DependencyInfo> dependencies;

        public MarketplaceVersion(
            String id, String name, String versionNumber, String channel, String datePublished,
            List<String> gameVersions, List<String> loaders, long downloads, String fileName,
            long fileSize, String downloadUrl, String sha1, boolean compatible, List<DependencyInfo> dependencies
        ) {
            this.id = id;
            this.name = name;
            this.versionNumber = versionNumber;
            this.channel = channel;
            this.datePublished = datePublished;
            this.gameVersions = gameVersions;
            this.loaders = loaders;
            this.downloads = downloads;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.downloadUrl = downloadUrl;
            this.sha1 = sha1;
            this.compatible = compatible;
            this.dependencies = dependencies;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getVersionNumber() { return versionNumber; }
        public String getChannel() { return channel; }
        public String getDatePublished() { return datePublished; }
        public List<String> getGameVersions() { return gameVersions; }
        public List<String> getLoaders() { return loaders; }
        public long getDownloads() { return downloads; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getSha1() { return sha1; }
        public boolean isCompatible() { return compatible; }
        public List<DependencyInfo> getDependencies() { return dependencies; }
    }

    public static class ProjectDetail {
        private final String id;
        private final String slug;
        private final String title;
        private final String author;
        private final String description;
        private final String iconUrl;
        private final String projectUrl;
        private final String sourceUrl;
        private final String projectType;
        private final String updatedAt;
        private final long downloads;
        private final long follows;
        private final List<String> categories;
        private final List<String> versionIds;
        private final List<MarketplaceVersion> versions;
        private final boolean versionsFilteredByGame;

        public ProjectDetail(
            String id, String slug, String title, String author, String description, String iconUrl,
            String projectUrl, String sourceUrl, String projectType, String updatedAt,
            long downloads, long follows, List<String> categories, List<String> versionIds,
            List<MarketplaceVersion> versions, boolean versionsFilteredByGame
        ) {
            this.id = id;
            this.slug = slug;
            this.title = title;
            this.author = author;
            this.description = description;
            this.iconUrl = iconUrl;
            this.projectUrl = projectUrl;
            this.sourceUrl = sourceUrl;
            this.projectType = projectType;
            this.updatedAt = updatedAt;
            this.downloads = downloads;
            this.follows = follows;
            this.categories = categories;
            this.versionIds = versionIds;
            this.versions = versions;
            this.versionsFilteredByGame = versionsFilteredByGame;
        }

        public String getId() { return id; }
        public String getSlug() { return slug; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getDescription() { return description; }
        public String getIconUrl() { return iconUrl; }
        public String getProjectUrl() { return projectUrl; }
        public String getSourceUrl() { return sourceUrl; }
        public String getProjectType() { return projectType; }
        public String getUpdatedAt() { return updatedAt; }
        public long getDownloads() { return downloads; }
        public long getFollows() { return follows; }
        public List<String> getCategories() { return categories; }
        public List<String> getVersionIds() { return versionIds; }
        public List<MarketplaceVersion> getVersions() { return versions; }
        public boolean isVersionsFilteredByGame() { return versionsFilteredByGame; }
    }

    public static class DependencyInfo {
        private final String projectId;
        private final String dependencyType;
        private final String versionId;
        private final String projectTitle;

        public DependencyInfo(String projectId, String dependencyType, String versionId, String projectTitle) {
            this.projectId = projectId;
            this.dependencyType = dependencyType;
            this.versionId = versionId;
            this.projectTitle = projectTitle;
        }

        public String getProjectId() { return projectId; }
        public String getDependencyType() { return dependencyType; }
        public String getVersionId() { return versionId; }
        public String getProjectTitle() { return projectTitle; }
    }

    public static class SelectedFile {
        private final String projectId;
        private final String projectTitle;
        private final String versionId;
        private final String versionNumber;
        private final String fileName;
        private final long size;
        private final String url;
        private final String sha1;

        public SelectedFile(
            String projectId, String projectTitle, String versionId, String versionNumber,
            String fileName, long size, String url, String sha1
        ) {
            this.projectId = projectId;
            this.projectTitle = projectTitle;
            this.versionId = versionId;
            this.versionNumber = versionNumber;
            this.fileName = fileName;
            this.size = size;
            this.url = url;
            this.sha1 = sha1;
        }

        public String getProjectId() { return projectId; }
        public String getProjectTitle() { return projectTitle; }
        public String getVersionId() { return versionId; }
        public String getVersionNumber() { return versionNumber; }
        public String getFileName() { return fileName; }
        public long getSize() { return size; }
        public String getUrl() { return url; }
        public String getSha1() { return sha1; }
    }

    public static class InstallPreview {
        private final SelectedFile target;
        private final List<SelectedFile> missingDependencies;
        private final List<DependencyInfo> alreadyInstalled;
        private final List<DependencyInfo> unresolvedDependencies;

        public InstallPreview(
            SelectedFile target,
            List<SelectedFile> missingDependencies,
            List<DependencyInfo> alreadyInstalled,
            List<DependencyInfo> unresolvedDependencies
        ) {
            this.target = target;
            this.missingDependencies = missingDependencies;
            this.alreadyInstalled = alreadyInstalled;
            this.unresolvedDependencies = unresolvedDependencies;
        }

        public SelectedFile getTarget() { return target; }
        public List<SelectedFile> getMissingDependencies() { return missingDependencies; }
        public List<DependencyInfo> getAlreadyInstalled() { return alreadyInstalled; }
        public List<DependencyInfo> getUnresolvedDependencies() { return unresolvedDependencies; }
    }

    public static class InstallEntry {
        private final String projectId;
        private final String versionId;

        public InstallEntry(String projectId, String versionId) {
            this.projectId = projectId;
            this.versionId = versionId;
        }

        public String getProjectId() { return projectId; }
        public String getVersionId() { return versionId; }
    }

    public static class InstallResult {
        private final List<SelectedFile> installed;
        private final boolean requiresRestart;

        public InstallResult(List<SelectedFile> installed, boolean requiresRestart) {
            this.installed = installed;
            this.requiresRestart = requiresRestart;
        }

        public List<SelectedFile> getInstalled() { return installed; }
        public boolean isRequiresRestart() { return requiresRestart; }
    }

    private static final class DependencyRef {
        private final String projectId;
        private final String versionId;
        private final int depth;

        private DependencyRef(String projectId, String versionId, int depth) {
            this.projectId = projectId;
            this.versionId = versionId;
            this.depth = depth;
        }
    }

    private static final class VersionResponse {
        private final JsonObject version;
        private final boolean mcimBased;

        private VersionResponse(JsonObject version, boolean mcimBased) {
            this.version = version;
            this.mcimBased = mcimBased;
        }
    }

    private static final class VersionListResponse {
        private final JsonArray versions;
        private final boolean mcimBased;

        private VersionListResponse(JsonArray versions, boolean mcimBased) {
            this.versions = versions;
            this.mcimBased = mcimBased;
        }
    }

    private static final class PendingInstall {
        private final SelectedFile file;
        private final Path temp;
        private final Path target;
        private final Path disabled;

        private PendingInstall(SelectedFile file, Path temp, Path target, Path disabled) {
            this.file = file;
            this.temp = temp;
            this.target = target;
            this.disabled = disabled;
        }
    }
}
