package net.opanel.backup.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class LocalBackupProvider implements BackupProvider {
    private final Path rootPath;

    public LocalBackupProvider(Path rootPath) {
        this.rootPath = rootPath.toAbsolutePath().normalize();
    }

    @Override
    public void testConnection() throws Exception {
        ensureRootExists();
    }

    @Override
    public void upload(Path sourcePath, String remoteKey, Map<String, String> metadata) throws Exception {
        ensureRootExists();
        Path targetPath = resolveObjectPath(remoteKey);
        Path parent = targetPath.getParent();
        if(parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void download(String remoteKey, Path targetPath) throws Exception {
        ensureRootExists();
        Path sourcePath = resolveObjectPath(remoteKey);
        if(!Files.exists(sourcePath)) {
            throw new IOException("Local backup object does not exist: " + remoteKey);
        }

        Path parent = targetPath.getParent();
        if(parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void delete(String remoteKey) throws Exception {
        ensureRootExists();
        Path targetPath = resolveObjectPath(remoteKey);
        Files.deleteIfExists(targetPath);
    }

    private void ensureRootExists() throws IOException {
        Files.createDirectories(rootPath);
    }

    private Path resolveObjectPath(String remoteKey) {
        if(remoteKey == null || remoteKey.isBlank()) {
            throw new IllegalArgumentException("Remote key is missing.");
        }

        String normalizedKey = remoteKey.replace('\\', '/');
        while(normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }

        Path targetPath = rootPath.resolve(normalizedKey).normalize();
        if(!targetPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("Invalid remote key.");
        }
        return targetPath;
    }
}
