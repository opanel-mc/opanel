package net.opanel.backup.provider;

import net.opanel.backup.BackupWebDavConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class WebDavBackupProvider implements BackupProvider {
    private final BackupWebDavConfig config;
    private final HttpClient httpClient;

    public WebDavBackupProvider(BackupWebDavConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public void testConnection() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(getBaseUrl()))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", authHeader())
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(resp.statusCode(), "Cannot connect WebDAV service.");
    }

    @Override
    public void upload(Path sourcePath, String remoteKey, Map<String, String> metadata) throws Exception {
        String remoteUrl = toObjectUrl(remoteKey);
        ensureParentDirectories(remoteUrl);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(remoteUrl))
                .timeout(Duration.ofMinutes(30))
                .header("Authorization", authHeader())
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofFile(sourcePath));
        for(Map.Entry<String, String> entry : metadata.entrySet()) {
            builder.header("X-OPanel-" + entry.getKey(), entry.getValue());
        }

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        ensureSuccess(resp.statusCode(), "WebDAV upload failed.");
    }

    @Override
    public void download(String remoteKey, Path targetPath) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(toObjectUrl(remoteKey)))
                .timeout(Duration.ofMinutes(30))
                .header("Authorization", authHeader())
                .GET()
                .build();
        HttpResponse<Path> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofFile(targetPath));
        ensureSuccess(resp.statusCode(), "WebDAV download failed.");
    }

    @Override
    public void delete(String remoteKey) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(toObjectUrl(remoteKey)))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", authHeader())
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if(resp.statusCode() == 404) return;
        ensureSuccess(resp.statusCode(), "WebDAV delete failed.");
    }

    private void ensureParentDirectories(String objectUrl) throws Exception {
        int protocolPos = objectUrl.indexOf("://");
        if(protocolPos < 0) return;

        int pathStart = objectUrl.indexOf('/', protocolPos + 3);
        if(pathStart < 0) return;

        String prefix = objectUrl.substring(0, pathStart);
        String fullPath = objectUrl.substring(pathStart + 1);
        String[] pathParts = fullPath.split("/");
        StringBuilder currentPath = new StringBuilder();
        for(int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            if(part.isBlank()) continue;

            if(currentPath.length() > 0) currentPath.append('/');
            currentPath.append(part);

            HttpRequest req = HttpRequest.newBuilder(URI.create(prefix + "/" + currentPath))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", authHeader())
                    .method("MKCOL", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if(code == 201 || code == 301 || code == 302 || code == 405) continue;
            if(code == 409) continue;
            ensureSuccess(code, "WebDAV MKCOL failed.");
        }
    }

    private String authHeader() {
        String raw = config.username + ":" + config.password;
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String getBaseUrl() {
        String base = config.baseUrl == null ? "" : config.baseUrl.trim();
        while(base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String toObjectUrl(String remoteKey) {
        String base = getBaseUrl();
        String rootPath = config.rootPath == null ? "" : config.rootPath.trim();
        while(rootPath.startsWith("/")) {
            rootPath = rootPath.substring(1);
        }
        while(rootPath.endsWith("/")) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }

        String key = remoteKey == null ? "" : remoteKey.trim();
        while(key.startsWith("/")) {
            key = key.substring(1);
        }

        if(rootPath.isEmpty()) {
            return base + "/" + key;
        }
        return base + "/" + rootPath + "/" + key;
    }

    private void ensureSuccess(int statusCode, String errMsg) throws IOException {
        if(statusCode >= 200 && statusCode < 300) return;
        throw new IOException(errMsg + " HTTP status: " + statusCode);
    }
}
