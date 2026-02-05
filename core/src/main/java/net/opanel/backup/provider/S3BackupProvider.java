package net.opanel.backup.provider;

import net.opanel.backup.BackupConfiguration;
import net.opanel.backup.BackupInfo;
import net.opanel.backup.BackupProvider;
import net.opanel.backup.BackupProviderType;
import net.opanel.utils.CryptoUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S3-compatible object storage backup provider.
 * Implements AWS Signature V2 for authentication.
 * Compatible with AWS S3, MinIO, Alibaba Cloud OSS, and other S3-compatible
 * services.
 */
public class S3BackupProvider implements BackupProvider {
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private final String prefix;

    public S3BackupProvider(BackupConfiguration config, String salt) {
        this.endpoint = config.s3Endpoint;
        this.accessKey = CryptoUtils.decrypt(config.s3AccessKeyEncrypted, salt);
        this.secretKey = CryptoUtils.decrypt(config.s3SecretKeyEncrypted, salt);
        this.bucket = config.s3Bucket;
        this.prefix = config.s3Prefix;
    }

    @Override
    public BackupProviderType getProviderType() {
        return BackupProviderType.S3;
    }

    @Override
    public void upload(Path zipFile, String fileName) throws IOException {
        String objectKey = getObjectKey(fileName);
        String url = buildUrl(objectKey);
        byte[] fileBytes = Files.readAllBytes(zipFile);

        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-Type", "application/zip");
        headers.put("Content-Length", String.valueOf(fileBytes.length));

        String date = iso8601Date(ZonedDateTime.now(ZoneOffset.UTC));
        headers.put("Date", date);
        String authorization = buildAuthorizationHeaderV2("PUT", "application/zip", date,
                canonicalizedResource(objectKey));
        headers.put("Authorization", authorization);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(fileBytes);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("S3 upload failed with status " + responseCode + ": " + readErrorStream(conn));
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public void download(String fileName, Path targetPath) throws IOException {
        String objectKey = getObjectKey(fileName);
        String url = buildUrl(objectKey);

        Map<String, String> headers = new TreeMap<>();
        String date = iso8601Date(ZonedDateTime.now(ZoneOffset.UTC));
        headers.put("Date", date);
        String authorization = buildAuthorizationHeaderV2("GET", "", date, canonicalizedResource(objectKey));
        headers.put("Authorization", authorization);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 404) {
                throw new FileNotFoundException("Backup not found: " + fileName);
            }
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("S3 download failed with status " + responseCode + ": " + readErrorStream(conn));
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
        String objectKey = getObjectKey(fileName);
        String url = buildUrl(objectKey);

        Map<String, String> headers = new TreeMap<>();
        String date = iso8601Date(ZonedDateTime.now(ZoneOffset.UTC));
        headers.put("Date", date);
        String authorization = buildAuthorizationHeaderV2("DELETE", "", date, canonicalizedResource(objectKey));
        headers.put("Authorization", authorization);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("DELETE");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300 && responseCode != 404) {
                throw new IOException("S3 delete failed with status " + responseCode + ": " + readErrorStream(conn));
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public List<BackupInfo> list() throws IOException {
        List<BackupInfo> backups = new ArrayList<>();
        String listPrefix = prefix.isEmpty() ? "" : prefix + "/";
        String url = buildUrl("") + "?list-type=2&prefix=" + urlEncode(listPrefix);

        Map<String, String> headers = new TreeMap<>();
        String date = iso8601Date(ZonedDateTime.now(ZoneOffset.UTC));
        headers.put("Date", date);
        String authorization = buildAuthorizationHeaderV2("GET", "", date, canonicalizedResource(""));
        headers.put("Authorization", authorization);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("S3 list failed with status " + responseCode + ": " + readErrorStream(conn));
            }

            String response = readInputStream(conn.getInputStream());
            backups = parseListResponse(response);
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
        return !endpoint.isEmpty() && !accessKey.isEmpty() && !secretKey.isEmpty() && !bucket.isEmpty();
    }

    private String getObjectKey(String fileName) {
        if (prefix.isEmpty()) {
            return fileName;
        }
        return prefix + "/" + fileName;
    }

    private String canonicalizedResource(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return "/" + bucket + "/";
        }
        return "/" + bucket + "/" + objectKey;
    }

    private String buildUrl(String objectKey) {
        String cleanEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        if (objectKey.isEmpty()) {
            return cleanEndpoint + "/" + bucket + "/";
        }
        return cleanEndpoint + "/" + bucket + "/" + objectKey;
    }

    private String buildAuthorizationHeaderV2(String method, String contentType, String date,
            String canonicalizedResource) {
        String stringToSign = method + "\n" +
                "\n" +
                contentType + "\n" +
                date + "\n" +
                canonicalizedResource;

        String signature = hmacSha1Base64(secretKey, stringToSign);
        return "AWS " + accessKey + ":" + signature;
    }

    private String hmacSha1Base64(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA1 failed", e);
        }
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    private String iso8601Date(ZonedDateTime now) {
        return DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .withLocale(Locale.ENGLISH)
                .format(now);
    }

    private List<BackupInfo> parseListResponse(String xml) {
        List<BackupInfo> backups = new ArrayList<>();

        // Simple XML parsing for S3 ListObjectsV2 response
        Pattern keyPattern = Pattern.compile("<Key>([^<]+)</Key>");
        Pattern sizePattern = Pattern.compile("<Size>(\\d+)</Size>");

        Matcher keyMatcher = keyPattern.matcher(xml);
        Matcher sizeMatcher = sizePattern.matcher(xml);

        while (keyMatcher.find() && sizeMatcher.find()) {
            String key = keyMatcher.group(1);
            long size = Long.parseLong(sizeMatcher.group(1));

            // Extract filename from key
            String fileName = key;
            if (key.contains("/")) {
                fileName = key.substring(key.lastIndexOf("/") + 1);
            }

            if (fileName.endsWith(".zip")) {
                backups.add(new BackupInfo(fileName, size));
            }
        }

        return backups;
    }

    private String readInputStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
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
