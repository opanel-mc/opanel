package net.opanel.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.opanel.OPanel;
import net.opanel.common.ServerType;

import net.opanel.exception.MarketplaceRateLimitException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Modrinth API client used by both the update detection providers and the
 * marketplace. The server owner picks the preferred endpoint ({@link #SOURCE_MCIM}
 * mirror, the official API, or both with the mirror first), and every request
 * transparently falls back to the next endpoint when the preferred one fails.
 */
public class ModrinthApi {
    public static final String SOURCE_MCIM = "mcim";
    public static final String SOURCE_MODRINTH = "modrinth";
    public static final String SOURCE_BOTH = "both";

    private static final String MCIM_API_BASE_URL = "https://mod.mcimirror.top/modrinth/v2";
    private static final String MODRINTH_API_BASE_URL = "https://api.modrinth.com/v2";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");

    private final HttpClient httpClient;
    private volatile List<String> apiBaseUrls;
    private volatile String lastResponseBaseUrl;

    public ModrinthApi() {
        this(SOURCE_BOTH);
    }

    public ModrinthApi(String source) {
        this.apiBaseUrls = resolveBaseUrls(normalizeSource(source));
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Switches which API endpoints the client talks to: {@link #SOURCE_MCIM}
     * (MCIM mirror only), {@link #SOURCE_MODRINTH} (official Modrinth only) or
     * {@link #SOURCE_BOTH} (MCIM first, falling back to official Modrinth).
     */
    public void setSource(String source) {
        this.apiBaseUrls = resolveBaseUrls(normalizeSource(source));
    }

    public static boolean isValidSource(String source) {
        return SOURCE_MCIM.equals(source) || SOURCE_MODRINTH.equals(source) || SOURCE_BOTH.equals(source);
    }

    /** The API base URL that answered the latest request, or {@code null}. */
    public String getLastResponseBaseUrl() {
        return lastResponseBaseUrl;
    }

    /** Whether the latest response was served by the MCIM mirror. */
    public boolean isMcimBased() {
        return isMcimBaseUrl(lastResponseBaseUrl);
    }

    public JsonElement getJson(String endpoint) throws IOException {
        ApiResponse<JsonElement> response = getJsonResponse(endpoint);
        lastResponseBaseUrl = response.getBaseUrl();
        return response.getBody();
    }

    public ApiResponse<JsonElement> getJsonResponse(String endpoint) throws IOException {
        IOException lastError = null;
        for(String apiBaseUrl : apiBaseUrls) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiBaseUrl + endpoint))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (github.com/opanel-mc/opanel)")
                    .GET()
                    .build();
                JsonElement response = send(request);
                return new ApiResponse<>(response, apiBaseUrl);
            } catch (MarketplaceRateLimitException e) {
                throw e; // Do not retry on rate limiting.
            } catch (IOException | RuntimeException e) {
                lastError = e instanceof IOException
                    ? (IOException) e
                    : new IOException("Invalid response from Modrinth API", e);
            }
        }
        throw lastError == null ? new IOException("No Modrinth API endpoint is configured") : lastError;
    }

    public JsonObject postJson(String endpoint, JsonObject body) throws IOException {
        ApiResponse<JsonObject> response = postJsonResponse(endpoint, body);
        lastResponseBaseUrl = response.getBaseUrl();
        return response.getBody();
    }

    public ApiResponse<JsonObject> postJsonResponse(String endpoint, JsonObject body) throws IOException {
        IOException lastError = null;
        for(String apiBaseUrl : apiBaseUrls) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiBaseUrl + endpoint))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (github.com/opanel-mc/opanel)")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
                JsonElement response = send(request);
                return new ApiResponse<>(response.isJsonObject() ? response.getAsJsonObject() : new JsonObject(), apiBaseUrl);
            } catch (MarketplaceRateLimitException e) {
                throw e; // Do not retry on rate limiting.
            } catch (IOException | RuntimeException e) {
                lastError = e instanceof IOException
                    ? (IOException) e
                    : new IOException("Invalid response from Modrinth API", e);
            }
        }
        throw lastError == null ? new IOException("No Modrinth API endpoint is configured") : lastError;
    }

    private JsonElement send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 429) {
                throw new MarketplaceRateLimitException();
            }
            if(response.statusCode() != 200) {
                throw new IOException("Modrinth API returned HTTP " + response.statusCode());
            }
            return JsonParser.parseString(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting the Modrinth API", e);
        }
    }

    /** A parsed API response together with the endpoint that supplied it. */
    public static class ApiResponse<T> {
        private final T body;
        private final String baseUrl;

        public ApiResponse(T body, String baseUrl) {
            this.body = body;
            this.baseUrl = baseUrl;
        }

        public T getBody() {
            return body;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public boolean isMcimBased() {
            return ModrinthApi.isMcimBaseUrl(baseUrl);
        }
    }

    private static String normalizeSource(String source) {
        if(source == null) return SOURCE_BOTH;
        String normalized = source.trim();
        if(SOURCE_MCIM.equalsIgnoreCase(normalized)) return SOURCE_MCIM;
        if(SOURCE_MODRINTH.equalsIgnoreCase(normalized)) return SOURCE_MODRINTH;
        return SOURCE_BOTH;
    }

    private static List<String> resolveBaseUrls(String source) {
        if(SOURCE_MODRINTH.equals(source)) return List.of(MODRINTH_API_BASE_URL);
        if(SOURCE_MCIM.equals(source)) return List.of(MCIM_API_BASE_URL);
        return List.of(MCIM_API_BASE_URL, MODRINTH_API_BASE_URL);
    }

    public static boolean isMcimBaseUrl(String apiBaseUrl) {
        return apiBaseUrl != null && apiBaseUrl.contains("mcimirror");
    }

    /**
     * The Modrinth loader tags a server of the given type should be filtered by.
     * Paper-based servers accept the whole Bukkit family because plugins usually
     * only tag the original loader they were built for.
     */
    public static List<String> modrinthLoaders(ServerType serverType) {
        if(serverType == null) {
            return List.of("fabric", "forge", "neoforge", "quilt", "paper", "purpur", "spigot", "bukkit", "folia");
        }
        switch(serverType) {
            case FABRIC:
                return List.of("fabric");
            case FORGE:
                return List.of("forge");
            case NEOFORGE:
                return List.of("neoforge");
            case FOLIA:
                return List.of("folia", "paper", "purpur", "spigot", "bukkit");
            case PAPER:
            case LEAVES:
                return List.of("paper", "purpur", "spigot", "bukkit");
            default:
                return List.of();
        }
    }

    /** Extracts the Minecraft game version (e.g. "1.21.4") from a server version string. */
    public static String extractMcVersion(String serverVersion) {
        if(serverVersion == null || serverVersion.isEmpty()) return null;
        Matcher matcher = MC_VERSION_PATTERN.matcher(serverVersion);
        return matcher.find() ? matcher.group() : null;
    }

    /** Rewrites Modrinth CDN links (downloads and icons) to the MCIM mirror. */
    public static String rewriteCdnUrl(String url) {
        if(url == null || url.isEmpty()) return url;
        return url.replace("://cdn.modrinth.com/", "://mod.mcimirror.top/");
    }
}
