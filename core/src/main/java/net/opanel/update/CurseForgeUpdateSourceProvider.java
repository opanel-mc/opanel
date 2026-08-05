package net.opanel.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.ServerType;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CurseForge update source provider.
 *
 * <p>Installed files are identified automatically via the official fingerprint
 * API. A CurseForge fingerprint is the <b>MurmurHash2 (seed 1)</b> of the file
 * bytes with every ASCII whitespace byte ({@code 0x09, 0x0A, 0x0D, 0x20})
 * removed. The result is a 32-bit unsigned integer submitted as a decimal
 * integer to {@code POST /v1/fingerprints}.</p>
 */
public class CurseForgeUpdateSourceProvider implements UpdateSourceProvider {
    private static final String API_BASE_URL = "https://mod.mcimirror.top/curseforge/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final Set<Integer> WHITESPACE_BYTES = Set.of(0x09, 0x0A, 0x0D, 0x20);

    private static final String[] PLATFORM_NAMES = { "Paper", "Spigot", "Bukkit" };

    private final String apiKey;
    private final HttpClient httpClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "opanel-mcim-curseforge-update-checker");
        thread.setDaemon(true);
        return thread;
    });

    public CurseForgeUpdateSourceProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public String getSource() {
        return "curseforge";
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
        final Map<String, OPanelPlugin> pluginsByName = new HashMap<>();
        for(OPanelPlugin plugin : plugins) pluginsByName.put(plugin.getFileName(), plugin);

        final Map<Long, String> fingerprintToFileName = computeFingerprints(pluginsPath, plugins);
        if(fingerprintToFileName.isEmpty()) return new ArrayList<>();

        final List<JsonObject> exactMatches = sendFingerprints(fingerprintToFileName.keySet());
        if(exactMatches.isEmpty()) return new ArrayList<>();

        final List<InstalledFile> installed = new ArrayList<>();
        for(JsonObject match : exactMatches) {
            if(!match.has("file") || match.get("file").isJsonNull()) continue;
            JsonObject file = match.getAsJsonObject("file");
            if(!file.has("fileFingerprint") || file.get("fileFingerprint").isJsonNull()) continue;
            String fileName = fingerprintToFileName.get(file.get("fileFingerprint").getAsLong());
            if(fileName == null) continue;
            installed.add(new InstalledFile(fileName, file));
        }
        if(installed.isEmpty()) return new ArrayList<>();

        final String mcVersion = extractMcVersion(serverVersion);
        final String loaderName = loaderName(serverType);
        final int loaderTypeId = loaderTypeId(serverType);
        final boolean paperSeries = isPaperSeries(serverType);

        final List<PluginUpdate> result = new ArrayList<>();
        final Queue<IOException> errors = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for(InstalledFile entry : installed) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    final PluginUpdate update = resolveUpdate(
                        entry,
                        pluginsByName,
                        fileHashes.get(entry.fileName),
                        mcVersion,
                        loaderName,
                        loaderTypeId,
                        paperSeries
                    );
                    if(update != null) {
                        synchronized(result) {
                            result.add(update);
                        }
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

    private PluginUpdate resolveUpdate(
        InstalledFile entry,
        Map<String, OPanelPlugin> pluginsByName,
        String installedSha1,
        String mcVersion,
        String loaderName,
        int loaderTypeId,
        boolean paperSeries
    ) throws IOException {
        final JsonObject installedFile = entry.file;
        if(!installedFile.has("modId") || installedFile.get("modId").isJsonNull()) return null;
        final String modId = installedFile.get("modId").getAsString();
        final long installedFileId = installedFile.has("id") && !installedFile.get("id").isJsonNull()
            ? installedFile.get("id").getAsLong()
            : -1;
        final long installedDate = toEpochMillis(getString(installedFile, "fileDate", null));
        final int installedReleaseType = installedFile.has("releaseType") && !installedFile.get("releaseType").isJsonNull()
            ? installedFile.get("releaseType").getAsInt()
            : 1;

        final JsonObject mod = getDataObject("mods/" + modId);
        final String name = mod != null ? firstNonBlank(getString(mod, "name", null), getString(installedFile, "displayName", null)) : getString(installedFile, "displayName", null);

        final JsonArray candidates = fetchCandidateFiles(modId, mcVersion, loaderTypeId);
        final JsonObject target = pickTarget(
            candidates,
            mcVersion,
            loaderName,
            paperSeries,
            installedFileId,
            installedDate,
            installedReleaseType
        );
        if(target == null) return null;

        final String projectUrl = projectUrl(mod);
        final String sha1 = extractSha1(target);
        final OPanelPlugin plugin = pluginsByName.get(entry.fileName);
        final String currentVersion = getString(installedFile, "displayName", plugin == null || plugin.getVersion() == null ? "" : plugin.getVersion());

        return new PluginUpdate(
            entry.fileName,
            name,
            currentVersion,
            getString(target, "displayName", ""),
            toMcimDownloadUrl(getString(target, "downloadUrl", "")),
            projectUrl,
            sha1,
            installedSha1,
            getSource(),
            modId,
            false,
            false,
            channelName(installedReleaseType),
            sha1 == null ? null : "sha1",
            sha1
        );
    }

    private JsonArray fetchCandidateFiles(String modId, String mcVersion, int loaderTypeId) throws IOException {
        String url = "mods/" + modId + "/files?pageSize=50";
        if(mcVersion != null && !mcVersion.isEmpty()) {
            url += "&gameVersion=" + URLEncoder.encode(mcVersion, StandardCharsets.UTF_8);
        }
        if(loaderTypeId != 0) {
            url += "&modLoaderType=" + loaderTypeId;
        }

        JsonArray files = getDataArray(url);
        if(!files.isEmpty() || mcVersion == null || mcVersion.isEmpty()) return files;

        // The game version filter returned nothing (the mod may not tag the exact
        // Minecraft version) - fall back to the broadest list and filter in code.
        String broadUrl = "mods/" + modId + "/files?pageSize=100";
        return getDataArray(broadUrl);
    }

    private static JsonObject pickTarget(
        JsonArray files,
        String mcVersion,
        String loaderName,
        boolean paperSeries,
        long installedFileId,
        long installedDate,
        int installedReleaseType
    ) {
        JsonObject best = null;
        long bestDate = -1;
        for(JsonElement element : files) {
            if(!element.isJsonObject()) continue;
            final JsonObject file = element.getAsJsonObject();

            final long fileId = file.has("id") && !file.get("id").isJsonNull() ? file.get("id").getAsLong() : -1;
            if(fileId == installedFileId) continue;

            if(installedDate > 0) {
                long date = toEpochMillis(getString(file, "fileDate", null));
                if(date <= installedDate) continue;
            }

            if(!matchesGameVersion(file, mcVersion)) continue;
            if(!matchesLoader(file, loaderName, paperSeries)) continue;
            if(!isAllowedReleaseType(file, installedReleaseType)) continue;

            long date = toEpochMillis(getString(file, "fileDate", null));
            if(date > bestDate) {
                bestDate = date;
                best = file;
            }
        }
        return best;
    }

    private static boolean matchesGameVersion(JsonObject file, String mcVersion) {
        if(mcVersion == null || mcVersion.isEmpty()) return true;
        return containsString(getStringList(file, "gameVersions"), mcVersion);
    }

    private static boolean matchesLoader(JsonObject file, String loaderName, boolean paperSeries) {
        final List<String> gameVersions = getStringList(file, "gameVersions");
        if(paperSeries) {
            for(String platform : PLATFORM_NAMES) {
                if(containsString(gameVersions, platform)) return true;
            }
            return false;
        }
        if(loaderName == null || loaderName.isEmpty()) return true;
        // If the file tags any known loader it must match ours; otherwise the
        // modLoaderType query filter already selected it.
        for(String candidate : gameVersions) {
            String lower = candidate.toLowerCase(Locale.ROOT);
            if(isLoaderTag(lower)) return loaderName.equalsIgnoreCase(candidate);
        }
        return true;
    }

    private static boolean isLoaderTag(String lower) {
        return "forge".equals(lower) || "fabric".equals(lower) || "neoforge".equals(lower)
            || "quilt".equals(lower) || "liteloader".equals(lower) || "cauldron".equals(lower);
    }

    private static boolean isAllowedReleaseType(JsonObject file, int installedReleaseType) {
        final int releaseType = file.has("releaseType") && !file.get("releaseType").isJsonNull()
            ? file.get("releaseType").getAsInt()
            : 1;
        // 1 = release, 2 = beta, 3 = alpha
        if(installedReleaseType == 1) return releaseType == 1;
        if(installedReleaseType == 2) return releaseType != 3;
        return true;
    }

    private Map<Long, String> computeFingerprints(Path pluginsPath, List<OPanelPlugin> plugins) {
        final Map<Long, String> result = new HashMap<>();
        for(OPanelPlugin plugin : plugins) {
            Path path = resolvePluginFilePath(pluginsPath, plugin.getFileName());
            if(path == null) continue;
            try {
                long fingerprint = computeFingerprint(Files.readAllBytes(path));
                result.put(fingerprint, plugin.getFileName());
            } catch (IOException e) {
                // Skip files that cannot be read.
            }
        }
        return result;
    }

    private static Path resolvePluginFilePath(Path pluginsPath, String fileName) {
        Path path = pluginsPath.resolve(fileName);
        if(Files.exists(path)) return path;
        Path disabledPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        return Files.exists(disabledPath) ? disabledPath : null;
    }

    /**
     * Computes the CurseForge fingerprint of a file: MurmurHash2 (seed 1) over
     * the bytes with every ASCII whitespace byte removed.
     */
    static long computeFingerprint(byte[] source) {
        byte[] normalized = stripWhitespace(source);
        return Integer.toUnsignedLong(murmurHash2(normalized, 1));
    }

    private static byte[] stripWhitespace(byte[] source) {
        List<Byte> filtered = new ArrayList<>(source.length);
        for(byte b : source) {
            if(!WHITESPACE_BYTES.contains((int) b)) filtered.add(b);
        }
        byte[] result = new byte[filtered.size()];
        for(int i = 0; i < filtered.size(); i++) result[i] = filtered.get(i);
        return result;
    }

    /** Standard MurmurHash2 (32-bit) by Austin Appleby. */
    private static int murmurHash2(byte[] data, int seed) {
        final int m = 0x5bd1e995;
        final int r = 24;

        int length = data.length;
        int h = seed ^ length;
        int i = 0;

        while(length - i >= 4) {
            int k = (data[i] & 0xff)
                | ((data[i + 1] & 0xff) << 8)
                | ((data[i + 2] & 0xff) << 16)
                | ((data[i + 3] & 0xff) << 24);

            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
            i += 4;
        }

        switch(length - i) {
            case 3:
                h ^= (data[i + 2] & 0xff) << 16;
            case 2:
                h ^= (data[i + 1] & 0xff) << 8;
            case 1:
                h ^= data[i] & 0xff;
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;
        return h;
    }

    private List<JsonObject> sendFingerprints(Set<Long> fingerprints) throws IOException {
        JsonArray body = new JsonArray();
        for(long fingerprint : fingerprints) body.add(fingerprint);

        JsonObject bodyWrapper = new JsonObject();
        bodyWrapper.add("fingerprints", body);

        JsonObject data = postDataObject("fingerprints", bodyWrapper);
        List<JsonObject> result = new ArrayList<>();
        if(data != null && data.has("exactMatches") && data.get("exactMatches").isJsonArray()) {
            for(JsonElement element : data.getAsJsonArray("exactMatches")) {
                if(element.isJsonObject()) result.add(element.getAsJsonObject());
            }
        }
        return result;
    }

    private JsonObject getDataObject(String endpoint) throws IOException {
        JsonElement data = getRaw(endpoint);
        return data == null || !data.isJsonObject() ? null : data.getAsJsonObject();
    }

    private JsonArray getDataArray(String endpoint) throws IOException {
        JsonElement data = getRaw(endpoint);
        if(data == null || !data.isJsonArray()) return new JsonArray();
        return data.getAsJsonArray();
    }

    private JsonObject postDataObject(String endpoint, JsonObject body) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(API_BASE_URL + "/" + endpoint))
            .timeout(TIMEOUT)
            .header("User-Agent", "OPanel/" + OPanel.VERSION + " (github.com/opanel-mc/opanel)")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if(!apiKey.isEmpty()) {
            requestBuilder.header("X-API-Key", apiKey);
        }
        HttpRequest request = requestBuilder.build();
        JsonElement data = parseData(request);
        return data == null || !data.isJsonObject() ? null : data.getAsJsonObject();
    }

    private JsonElement getRaw(String endpoint) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(API_BASE_URL + "/" + endpoint))
            .timeout(TIMEOUT)
            .header("User-Agent", "OPanel/" + OPanel.VERSION + " (github.com/opanel-mc/opanel)")
            .header("Accept", "application/json");
        if(!apiKey.isEmpty()) {
            requestBuilder.header("X-API-Key", apiKey);
        }
        HttpRequest request = requestBuilder.GET().build();
        return parseData(request);
    }

    private JsonElement parseData(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) {
                if(response.statusCode() == 403) {
                    throw new IOException("MCIM CurseForge mirror rejected the request (HTTP 403).");
                }
                throw new IOException("MCIM CurseForge mirror returned HTTP " + response.statusCode());
            }
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            return root == null || !root.has("data") || root.get("data").isJsonNull()
                ? null
                : root.get("data");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting the MCIM CurseForge mirror", e);
        }
    }

    private static String toMcimDownloadUrl(String url) {
        if(url == null || url.isEmpty()) return url;
        return url
            .replace("://edge.forgecdn.net/", "://mod.mcimirror.top/")
            .replace("://mediafilez.forgecdn.net/", "://mod.mcimirror.top/")
            .replace("://mediafiles.forgecdn.net/", "://mod.mcimirror.top/");
    }

    private static String projectUrl(JsonObject mod) {
        if(mod != null && mod.has("links") && mod.get("links").isJsonObject()) {
            JsonObject links = mod.getAsJsonObject("links");
            String websiteUrl = getString(links, "websiteUrl", null);
            if(websiteUrl != null && !websiteUrl.isEmpty()) return websiteUrl;
        }
        String slug = mod == null ? null : getString(mod, "slug", null);
        if(slug != null && !slug.isEmpty()) {
            return "https://www.curseforge.com/minecraft/mc-mods/" + slug;
        }
        return "https://www.curseforge.com/minecraft";
    }

    private static String extractSha1(JsonObject target) {
        if(!target.has("hashes") || !target.get("hashes").isJsonArray()) return null;
        for(JsonElement element : target.getAsJsonArray("hashes")) {
            if(!element.isJsonObject()) continue;
            JsonObject hash = element.getAsJsonObject();
            if(hash.has("algo") && hash.get("algo").getAsInt() == 1) {
                return getString(hash, "value", null);
            }
        }
        return null;
    }

    private static boolean isPaperSeries(ServerType serverType) {
        return serverType != null && serverType.isPaperSeries();
    }

    private static String loaderName(ServerType serverType) {
        if(serverType == null) return null;
        switch(serverType) {
            case FABRIC:
                return "Fabric";
            case FORGE:
                return "Forge";
            case NEOFORGE:
                return "NeoForge";
            default:
                return null;
        }
    }

    private static int loaderTypeId(ServerType serverType) {
        if(serverType == null) return 0;
        switch(serverType) {
            case FORGE:
                return 1;
            case FABRIC:
                return 4;
            case NEOFORGE:
                return 6;
            default:
                return 0;
        }
    }

    private static String channelName(int releaseType) {
        if(releaseType == 2) return "beta";
        if(releaseType == 3) return "alpha";
        return "release";
    }

    private static String extractMcVersion(String serverVersion) {
        if(serverVersion == null || serverVersion.isEmpty()) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?")
            .matcher(serverVersion);
        return matcher.find() ? matcher.group() : null;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isEmpty() ? first : second;
    }

    private static boolean containsString(List<String> list, String value) {
        if(list == null || value == null) return false;
        for(String item : list) {
            if(value.equalsIgnoreCase(item)) return true;
        }
        return false;
    }

    private static List<String> getStringList(JsonObject object, String key) {
        List<String> result = new ArrayList<>();
        if(object != null && object.has(key) && object.get(key).isJsonArray()) {
            for(JsonElement element : object.getAsJsonArray(key)) {
                if(element.isJsonPrimitive()) result.add(element.getAsString());
            }
        }
        return result;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if(object != null && object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return fallback;
    }

    private static long toEpochMillis(String date) {
        if(date == null || date.isEmpty()) return 0;
        try {
            return OffsetDateTime.parse(date).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(date.replace("Z", "+00:00").replace("z", "+00:00")).toInstant().toEpochMilli();
            } catch (DateTimeParseException e2) {
                return 0;
            }
        }
    }

    private static final class InstalledFile {
        private final String fileName;
        private final JsonObject file;

        private InstalledFile(String fileName, JsonObject file) {
            this.fileName = fileName;
            this.file = file;
        }
    }
}
