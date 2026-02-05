package net.opanel.backup.provider;

import net.opanel.backup.BackupConfiguration;
import net.opanel.backup.BackupInfo;
import net.opanel.backup.BackupProvider;
import net.opanel.backup.BackupProviderType;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Local filesystem backup provider.
 * Stores backups in a local directory.
 */
public class LocalBackupProvider implements BackupProvider {
    private final Path backupDir;

    public LocalBackupProvider(BackupConfiguration config) {
        this.backupDir = Paths.get(config.localPath);
    }

    @Override
    public BackupProviderType getProviderType() {
        return BackupProviderType.LOCAL;
    }

    @Override
    public void upload(Path zipFile, String fileName) throws IOException {
        // Ensure backup directory exists
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        Path targetPath = backupDir.resolve(fileName);
        Files.copy(zipFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void download(String fileName, Path targetPath) throws IOException {
        Path sourcePath = backupDir.resolve(fileName);
        if (!Files.exists(sourcePath)) {
            throw new java.io.FileNotFoundException("Backup not found: " + fileName);
        }

        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void delete(String fileName) throws IOException {
        Path targetPath = backupDir.resolve(fileName);
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
        }
    }

    @Override
    public List<BackupInfo> list() throws IOException {
        List<BackupInfo> backups = new ArrayList<>();

        if (!Files.exists(backupDir)) {
            return backups;
        }

        try (Stream<Path> stream = Files.list(backupDir)) {
            stream.filter(path -> path.toString().endsWith(".zip"))
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            long size = Files.size(path);
                            backups.add(new BackupInfo(fileName, size));
                        } catch (IOException e) {
                            // Skip files we can't read
                        }
                    });
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
        return backupDir != null && !backupDir.toString().isEmpty();
    }
}
