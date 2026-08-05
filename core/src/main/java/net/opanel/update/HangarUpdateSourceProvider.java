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
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HangarUpdateSourceProvider implements UpdateSourceProvider {
    private static final String API_BASE_URL = "https://hangar.papermc.io/api/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");

    private final HttpClient httpClient;

    public HangarUpdateSourceProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public String getSource() {
        return "hangar";
    }

    @Override
    public boolean supportsAutomaticIdentification() {
        return false;
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
        List<PluginUpdate> result = new ArrayList<>();
        final String mcVersion = extractMcVersion(serverVersion);
        for(OPanelPlugin plugin : plugins) {
            PluginUpdateBinding binding = config.getBinding(plugin.getFileName());
            if(binding == null || !"hangar".equals(binding.getSource())) continue;
            if(binding.getProjectId() == null || binding.getProjectId().isBlank()) continue;

            final String installedSha1 = fileHashes.get(plugin.getFileName());
            if(installedSha1 == null) continue;

            final String platform = hangarPlatform(serverType);
            if(platform == null) continue;

            JsonArray versions = fetchVersions(binding.getProjectId(), platform);
            JsonObject version = pickTarget(versions, platform, mcVersion, binding);
            if(version == null) continue;

            JsonObject download = version.has("downloads") && version.get("downloads").isJsonObject()
                ? version.getAsJsonObject("downloads").getAsJsonObject(platform)
                : null;
            if(download == null || !download.has("downloadUrl") || download.get("downloadUrl").isJsonNull()) {
                continue;
            }

            final String downloadUrl = download.get("downloadUrl").getAsString();
            final String digest = extractSha256(download);
            final String channelName = version.has("channel") && version.get("channel").isJsonObject()
                ? getString(version.getAsJsonObject("channel"), "name", "Release")
                : "Release";
            result.add(new PluginUpdate(
                plugin.getFileName(),
                plugin.getName(),
                plugin.getVersion() == null ? "" : plugin.getVersion(),
                getString(version, "name", ""),
                downloadUrl,
                "https://hangar.papermc.io/"+ binding.getProjectId() +"/versions",
                null,
                installedSha1,
                getSource(),
                binding.getProjectId(),
                true,
                false,
                channelName.toLowerCase(Locale.ROOT).startsWith("release") ? "release" : "beta",
                digest == null ? null : "sha256",
                digest
            ));
        }
        return result;
    }

    private JsonArray fetchVersions(String projectId, String platform) throws IOException {
        final String encodedProject = URLEncoder.encode(projectId, StandardCharsets.UTF_8);
        final String url = API_BASE_URL +"/projects/"+ encodedProject +"/versions?limit=50&platform="+ platform;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (github.com/opanel-mc/opanel)")
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) {
                throw new IOException("Hangar API returned HTTP " + response.statusCode());
            }
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            if(body.has("result") && body.get("result").isJsonArray()) {
                return body.getAsJsonArray("result");
            }
            return new JsonArray();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting the Hangar API", e);
        }
    }

    private static JsonObject pickTarget(
        JsonArray versions,
        String platform,
        String mcVersion,
        PluginUpdateBinding binding
    ) {
        for(JsonElement element : versions) {
            JsonObject version = element.getAsJsonObject();
            if(!isChannelAllowed(binding.getChannels(), getString(version.has("channel") && version.get("channel").isJsonObject()
                ? version.getAsJsonObject("channel")
                : null, "name", "Release"))) {
                continue;
            }

            if(mcVersion != null && !supportsMcVersion(version, platform, mcVersion)) continue;
            return version;
        }
        return null;
    }

    private static boolean isChannelAllowed(List<String> channels, String channelName) {
        if(channels == null || channels.isEmpty()) return true;
        boolean release = "release".equalsIgnoreCase(channelName);
        boolean beta = !release && ("beta".equalsIgnoreCase(channelName) || "snapshot".equalsIgnoreCase(channelName));
        return channels.contains("release") && release
            || (channels.contains("beta") && !release && beta)
            || channels.contains("alpha") && beta;
    }

    private static boolean supportsMcVersion(JsonObject version, String platform, String mcVersion) {
        if(!version.has("platformDependencies") || !version.get("platformDependencies").isJsonObject()) return false;
        JsonObject deps = version.getAsJsonObject("platformDependencies");
        if(!deps.has(platform) || !deps.get(platform).isJsonArray()) return false;
        for(JsonElement element : deps.getAsJsonArray(platform)) {
            if(mcVersion.equals(element.getAsString())) return true;
        }
        return false;
    }

    private static String extractSha256(JsonObject download) {
        if(download.has("fileInfo") && download.get("fileInfo").isJsonObject()
            && !download.getAsJsonObject("fileInfo").get("sha256Hash").isJsonNull()) {
            return getString(download.getAsJsonObject("fileInfo"), "sha256Hash", null);
        }
        return null;
    }

    private static String hangarPlatform(ServerType serverType) {
        switch(serverType) {
            case FABRIC:
                return "FABRIC";
            case FORGE:
                return "FORGE";
            case NEOFORGE:
                return "NEOFORGE";
            case PAPER:
            case LEAVES:
            case FOLIA:
                return "PAPER";
            default:
                return null;
        }
    }

    private static String extractMcVersion(String serverVersion) {
        if(serverVersion == null || serverVersion.isEmpty()) return null;
        Matcher matcher = MC_VERSION_PATTERN.matcher(serverVersion);
        return matcher.find() ? matcher.group() : null;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if(object != null && object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return fallback;
    }
}
