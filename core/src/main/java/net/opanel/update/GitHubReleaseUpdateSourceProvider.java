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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GitHubReleaseUpdateSourceProvider implements UpdateSourceProvider {
    private static final String API_BASE_URL = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public GitHubReleaseUpdateSourceProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public String getSource() {
        return "github";
    }

    @Override
    public boolean supportsAutomaticIdentification() {
        return false;
    }

    @Override
    public boolean isAutoApplySafe() {
        return false;
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
        for(OPanelPlugin plugin : plugins) {
            PluginUpdateBinding binding = config.getBinding(plugin.getFileName());
            if(binding == null || !"github".equals(binding.getSource())) continue;
            if(binding.getOwner() == null || binding.getOwner().isBlank()
                || binding.getRepo() == null || binding.getRepo().isBlank()) {
                continue;
            }

            final String installedSha1 = fileHashes.get(plugin.getFileName());
            if(installedSha1 == null) continue;

            JsonArray releases = fetchReleases(binding.getOwner(), binding.getRepo());
            JsonObject release = findLatestRelease(releases, binding);
            if(release == null) continue;

            final JsonObject asset = findMatchingAsset(release, binding.getAssetPattern());
            if(asset == null) continue;

            final String tagName = getString(release, "tag_name", "");
            if(tagName.isEmpty()) continue;

            final boolean prerelease = release.has("prerelease") && release.get("prerelease").getAsBoolean();
            final String channel = prerelease ? "beta" : "release";
            result.add(new PluginUpdate(
                plugin.getFileName(),
                plugin.getName(),
                plugin.getVersion() == null ? "" : plugin.getVersion(),
                tagName,
                getString(asset, "browser_download_url", ""),
                "https://github.com/"+ binding.getOwner() +"/"+ binding.getRepo() +"/releases/tag/"+ tagName,
                null,
                installedSha1,
                getSource(),
                binding.getProjectId() == null || binding.getProjectId().isBlank()
                    ? binding.getOwner() +"/"+ binding.getRepo()
                    : binding.getProjectId(),
                true,
                false,
                channel,
                null,
                null
            ));
        }
        return result;
    }

    private JsonArray fetchReleases(String owner, String repo) throws IOException {
        final String url = API_BASE_URL +"/repos/"+ owner +"/"+ repo +"/releases?per_page=100";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (github.com/opanel-mc/opanel)")
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) {
                throw new IOException("GitHub Releases API returned HTTP " + response.statusCode());
            }
            JsonElement body = JsonParser.parseString(response.body());
            return body.isJsonArray() ? body.getAsJsonArray() : new JsonArray();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting the GitHub Releases API", e);
        }
    }

    private static JsonObject findLatestRelease(JsonArray releases, PluginUpdateBinding binding) {
        for(JsonElement element : releases) {
            JsonObject release = element.getAsJsonObject();
            if(release.has("draft") && release.get("draft").getAsBoolean()) continue;
            if(!isChannelAllowed(binding.getChannels(), release.has("prerelease") && release.get("prerelease").getAsBoolean())) continue;
            if(!release.has("assets") || !release.get("assets").isJsonArray()) continue;
            if(release.getAsJsonArray("assets").size() == 0) continue;
            if(findMatchingAsset(release, binding.getAssetPattern()) != null) return release;
        }
        return null;
    }

    private static JsonObject findMatchingAsset(JsonObject release, String assetPattern) {
        if(!release.has("assets") || !release.get("assets").isJsonArray()) return null;
        Pattern pattern = null;
        if(assetPattern != null && !assetPattern.isBlank()) {
            try {
                pattern = Pattern.compile(assetPattern, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                pattern = null;
            }
        }

        for(JsonElement element : release.getAsJsonArray("assets")) {
            JsonObject asset = element.getAsJsonObject();
            String name = getString(asset, "name", "");
            if(pattern == null) {
                if(name.endsWith(".jar")) return asset;
            } else if(pattern.matcher(name).matches()) {
                return asset;
            }
        }
        return null;
    }

    private static boolean isChannelAllowed(List<String> channels, boolean prerelease) {
        if(channels == null || channels.isEmpty()) return true;
        if(prerelease) {
            return channels.contains("beta") || channels.contains("alpha");
        }
        return channels.contains("release") || channels.contains("beta") || channels.contains("alpha");
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if(object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return fallback;
    }
}
