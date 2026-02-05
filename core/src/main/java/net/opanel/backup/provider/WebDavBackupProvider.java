package net.opanel.backup.provider;

import net.opanel.backup.BackupConfiguration;
import net.opanel.backup.BackupInfo;
import net.opanel.backup.BackupProvider;
import net.opanel.backup.BackupProviderType;
import net.opanel.utils.CryptoUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebDAV backup provider.
 * Uses HTTP/HTTPS to upload and manage backups on a WebDAV server.
 */
public class WebDavBackupProvider implements BackupProvider {
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 300000; // 5 minutes for large uploads

    private final String baseUrl;
    private final String username;
    private final String password;

    public WebDavBackupProvider(BackupConfiguration config, String salt) {
        String url = config.webdavUrl;
        // Ensure URL ends with /
        this.baseUrl = url.endsWith("/") ? url : url + "/";
        this.username = CryptoUtils.decrypt(config.webdavUsernameEncrypted, salt);
        this.password = CryptoUtils.decrypt(config.webdavPasswordEncrypted, salt);
    }

    @Override
    public BackupProviderType getProviderType() {
        return BackupProviderType.WEBDAV;
    }

    @Override
    public void upload(Path zipFile, String fileName) throws IOException {
        String url = baseUrl + fileName;
        byte[] fileBytes = Files.readAllBytes(zipFile);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Content-Type", "application/zip");
            conn.setRequestProperty("Content-Length", String.valueOf(fileBytes.length));
            setAuthHeader(conn);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(fileBytes);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(
                        "WebDAV upload failed with status " + responseCode + ": " + readErrorStream(conn));
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public void download(String fileName, Path targetPath) throws IOException {
        String url = baseUrl + fileName;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            setAuthHeader(conn);

            int responseCode = conn.getResponseCode();
            if (responseCode == 404) {
                throw new FileNotFoundException("Backup not found: " + fileName);
            }
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(
                        "WebDAV download failed with status " + responseCode + ": " + readErrorStream(conn));
            }

            Path parent = targetPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (InputStream is = conn.getInputStream()) {
                Files.copy(is, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public void delete(String fileName) throws IOException {
        String url = baseUrl + fileName;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            setAuthHeader(conn);

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300 && responseCode != 404) {
                throw new IOException(
                        "WebDAV delete failed with status " + responseCode + ": " + readErrorStream(conn));
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public List<BackupInfo> list() throws IOException {
        List<BackupInfo> backups = new ArrayList<>();

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl).openConnection();
        try {
            // Use PROPFIND method to list directory contents
            conn.setRequestMethod("PROPFIND");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Depth", "1");
            conn.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
            setAuthHeader(conn);

            // Send PROPFIND request body
            String propfindBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<D:propfind xmlns:D=\"DAV:\">" +
                    "<D:prop>" +
                    "<D:displayname/>" +
                    "<D:getcontentlength/>" +
                    "</D:prop>" +
                    "</D:propfind>";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(propfindBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("WebDAV list failed with status " + responseCode + ": " + readErrorStream(conn));
            }

            String response = readInputStream(conn.getInputStream());
            backups = parsePropfindResponse(response);
        } finally {
            conn.disconnect();
        }

        // Sort by timestamp descending (newest first)
        backups.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null)
                return 0;
            if (a.getTimestamp() == null)
                return 1;
            if (b.getTimestamp() == null)
                return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        return backups;
    }

    @Override
    public boolean isConfigured() {
        return !baseUrl.isEmpty() && !username.isEmpty() && !password.isEmpty();
    }

    private void setAuthHeader(HttpURLConnection conn) {
        if (!username.isEmpty() && !password.isEmpty()) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
    }

    private List<BackupInfo> parsePropfindResponse(String xml) {
        List<BackupInfo> backups = new ArrayList<>();

        // Simple XML parsing for WebDAV PROPFIND response
        // Look for <D:href> and <D:getcontentlength> elements
        Pattern responsePattern = Pattern.compile("<D:response>(.*?)</D:response>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Pattern hrefPattern = Pattern.compile("<D:href>([^<]+)</D:href>", Pattern.CASE_INSENSITIVE);
        Pattern sizePattern = Pattern.compile("<D:getcontentlength>(\\d+)</D:getcontentlength>",
                Pattern.CASE_INSENSITIVE);

        Matcher responseMatcher = responsePattern.matcher(xml);
        while (responseMatcher.find()) {
            String responseBlock = responseMatcher.group(1);

            Matcher hrefMatcher = hrefPattern.matcher(responseBlock);
            Matcher sizeMatcher = sizePattern.matcher(responseBlock);

            if (hrefMatcher.find()) {
                String href = hrefMatcher.group(1);
                long size = 0;
                if (sizeMatcher.find()) {
                    size = Long.parseLong(sizeMatcher.group(1));
                }

                // Extract filename from href
                String fileName = href;
                if (href.contains("/")) {
                    fileName = href.substring(href.lastIndexOf("/") + 1);
                }

                // URL decode the filename
                try {
                    fileName = java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8.toString());
                } catch (Exception e) {
                    // Keep original
                }

                if (fileName.endsWith(".zip") && !fileName.isEmpty()) {
                    backups.add(new BackupInfo(fileName, size));
                }
            }
        }

        return backups;
    }

    private String readInputStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String readErrorStream(HttpURLConnection conn) {
        try {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                return readInputStream(errorStream);
            }
        } catch (IOException e) {
            // Ignore
        }
        return "";
    }
}
