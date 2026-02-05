package net.opanel.backup;

import net.opanel.OPanel;
import net.opanel.backup.provider.LocalBackupProvider;
import net.opanel.backup.provider.S3BackupProvider;
import net.opanel.backup.provider.WebDavBackupProvider;
import net.opanel.common.OPanelSave;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;
import net.opanel.utils.ZipUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages backup operations for the server.
 * All backup operations are executed asynchronously to avoid blocking the main
 * server thread.
 */
public class BackupManager {
    private final OPanel plugin;
    private final ExecutorService executor;
    private BackupProvider provider;
    private BackupConfiguration config;

    public BackupManager(OPanel plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "OPanel-Backup-Thread");
            t.setDaemon(true);
            return t;
        });

        loadConfig();
    }

    /**
     * Loads or reloads the backup configuration from storage.
     */
    public void loadConfig() {
        this.config = Storage.get().getStoredData(StorageKey.BACKUP_CONFIG);
        if (this.config == null) {
            this.config = BackupConfiguration.defaultConfig;
        }
        updateProvider();
    }

    /**
     * Saves the current backup configuration to storage.
     */
    public void saveConfig() {
        Storage.get().setStoredData(StorageKey.BACKUP_CONFIG, config);
    }

    /**
     * Updates the backup provider based on current configuration.
     */
    private void updateProvider() {
        if (!config.enabled) {
            this.provider = null;
            return;
        }

        String salt = plugin.getConfig().salt;
        switch (config.getProviderTypeEnum()) {
            case LOCAL -> this.provider = new LocalBackupProvider(config);
            case S3 -> this.provider = new S3BackupProvider(config, salt);
            case WEBDAV -> this.provider = new WebDavBackupProvider(config, salt);
            default -> this.provider = new LocalBackupProvider(config);
        }
    }

    /**
     * Gets the current backup configuration.
     */
    public BackupConfiguration getConfig() {
        return config;
    }

    /**
     * Sets and saves a new backup configuration.
     */
    public void setConfig(BackupConfiguration config) {
        this.config = config;
        saveConfig();
        updateProvider();
    }

    /**
     * Checks if the backup system is properly configured and ready to use.
     */
    public boolean isConfigured() {
        return config.enabled && provider != null && provider.isConfigured();
    }

    /**
     * Performs a backup of the current world asynchronously.
     *
     * @param saveAllCallback A callback that should be executed on the main server
     *                        thread
     *                        to save all world data before backup. This ensures
     *                        world data
     *                        is flushed to disk.
     * @return A CompletableFuture containing the backup result
     */
    public CompletableFuture<BackupResult> performBackupAsync(Runnable saveAllCallback) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(
                    BackupResult.failure("Backup is not configured or disabled"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Call saveAll on the main thread (blocking wait)
                if (saveAllCallback != null) {
                    saveAllCallback.run();
                }

                // Step 2: Get current save/world info
                List<OPanelSave> saves = plugin.getServer().getSaves();
                OPanelSave currentSave = null;
                for (OPanelSave save : saves) {
                    try {
                        if (save.isCurrent()) {
                            currentSave = save;
                            break;
                        }
                    } catch (IOException e) {
                        // Continue checking other saves
                    }
                }

                if (currentSave == null) {
                    return BackupResult.failure("No current save found");
                }

                String saveName = currentSave.getName();
                Path savePath = currentSave.getPath();

                // Step 3: Create zip file
                String fileName = BackupInfo.generateFileName(saveName);
                Path zipPath = OPanel.TMP_DIR_PATH.resolve(UUID.randomUUID() + ".zip");

                try {
                    // Handle Bukkit's dimension separation
                    if (plugin.getServer().getServerType().isBukkitSeries()) {
                        Path netherDim = Paths.get("").resolve(saveName + "_nether/DIM-1");
                        Path theEndDim = Paths.get("").resolve(saveName + "_the_end/DIM1");
                        if (Files.exists(netherDim)) {
                            copyDirectory(netherDim, savePath.resolve("DIM-1"));
                        }
                        if (Files.exists(theEndDim)) {
                            copyDirectory(theEndDim, savePath.resolve("DIM1"));
                        }
                    }

                    ZipUtility.zip(savePath, zipPath);

                    // Step 4: Ensure retention before upload
                    prepareForNewBackup();

                    // Step 5: Upload to storage
                    long fileSize = Files.size(zipPath);
                    provider.upload(zipPath, fileName);

                    // Step 6: Cleanup old backups
                    cleanupOldBackups();

                    BackupInfo backupInfo = new BackupInfo(fileName, fileSize);
                    plugin.logger.info("Backup completed: " + fileName);

                    return BackupResult.success("Backup completed successfully", backupInfo);

                } finally {
                    // Cleanup temporary files
                    if (Files.exists(zipPath)) {
                        Files.delete(zipPath);
                    }

                    // Cleanup Bukkit dimension copies
                    if (plugin.getServer().getServerType().isBukkitSeries()) {
                        Path dimNether = savePath.resolve("DIM-1");
                        Path dimEnd = savePath.resolve("DIM1");
                        if (Files.exists(dimNether)) {
                            deleteDirectory(dimNether);
                        }
                        if (Files.exists(dimEnd)) {
                            deleteDirectory(dimEnd);
                        }
                    }
                }

            } catch (Exception e) {
                plugin.logger.error("Backup failed: " + e.getMessage());
                return BackupResult.failure("Backup failed: " + e.getMessage());
            }
        }, executor);
    }

    /**
     * Lists all available backups asynchronously.
     */
    public CompletableFuture<List<BackupInfo>> listBackupsAsync() {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return provider.list();
            } catch (IOException e) {
                plugin.logger.error("Failed to list backups: " + e.getMessage());
                return List.of();
            }
        }, executor);
    }

    /**
     * Deletes a backup asynchronously.
     */
    public CompletableFuture<Boolean> deleteBackupAsync(String fileName) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<BackupInfo> backups = provider.list();
                boolean exists = backups.stream().anyMatch(backup -> backup.getFileName().equals(fileName));
                if (!exists) {
                    return false;
                }

                provider.delete(fileName);
                plugin.logger.info("Backup deleted: " + fileName);
                return true;
            } catch (IOException e) {
                plugin.logger.error("Failed to delete backup: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * Restores a backup asynchronously.
     *
     * @param fileName The backup file name to restore
     * @return A CompletableFuture containing the restored save name
     */
    public CompletableFuture<String> restoreBackupAsync(String fileName) {
        if (!isConfigured()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Backup is not configured"));
        }

        return CompletableFuture.supplyAsync(() -> {
            Path zipPath = OPanel.TMP_DIR_PATH.resolve(UUID.randomUUID() + ".zip");
            Path extractPath = OPanel.TMP_DIR_PATH.resolve("restore-" + UUID.randomUUID());

            try {
                provider.download(fileName, zipPath);
                ZipUtility.unzip(zipPath, extractPath);

                Path sourcePath = resolveExtractedSaveDir(extractPath);
                if (sourcePath == null) {
                    throw new IOException("Invalid backup file.");
                }

                String restoreName = resolveRestoreName(fileName, sourcePath.getFileName().toString());
                Path restorePath = Paths.get("").resolve(restoreName);

                Utils.copyDirectoryRecursively(sourcePath, restorePath);

                if (plugin.getServer().getServerType().isBukkitSeries()) {
                    splitBukkitDimensions(restorePath, restoreName);
                }

                plugin.logger.info("Backup restored: " + fileName + " -> " + restoreName);
                return restoreName;
            } catch (IOException e) {
                plugin.logger.error("Backup restore failed: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                try {
                    if (Files.exists(zipPath)) {
                        Files.delete(zipPath);
                    }
                } catch (IOException e) {
                    // Ignore
                }

                try {
                    if (Files.exists(extractPath)) {
                        Utils.deleteDirectoryRecursively(extractPath);
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        }, executor);
    }

    /**
     * Cleans up old backups exceeding the maximum retention count.
     */
    private void cleanupOldBackups() {
        if (config.maxBackups <= 0) {
            return; // Unlimited backups
        }

        try {
            List<BackupInfo> backups = provider.list();
            if (backups.size() > config.maxBackups) {
                // Remove oldest backups (list is sorted newest first)
                for (int i = config.maxBackups; i < backups.size(); i++) {
                    BackupInfo oldBackup = backups.get(i);
                    try {
                        provider.delete(oldBackup.getFileName());
                        plugin.logger.info("Deleted old backup: " + oldBackup.getFileName());
                    } catch (IOException e) {
                        plugin.logger
                                .warn("Failed to delete old backup " + oldBackup.getFileName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            plugin.logger.warn("Failed to cleanup old backups: " + e.getMessage());
        }
    }

    private String resolveRestoreName(String fileName, String fallbackName) {
        String baseName = new BackupInfo(fileName, 0).getSaveName();
        if (baseName == null || baseName.isEmpty()) {
            baseName = (fallbackName == null || fallbackName.isEmpty()) ? "restored" : fallbackName;
        }

        String candidate = baseName;
        if (!saveNameExists(candidate)) {
            return candidate;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int counter = 0;
        while (true) {
            String suffix = counter == 0 ? "" : "-" + counter;
            candidate = baseName + "-restored-" + timestamp + suffix;
            if (!saveNameExists(candidate)) {
                return candidate;
            }
            counter++;
        }
    }

    private boolean saveNameExists(String saveName) {
        Path savePath = Paths.get("").resolve(saveName);
        if (Files.exists(savePath)) {
            return true;
        }
        if (plugin.getServer().getServerType().isBukkitSeries()) {
            Path netherPath = Paths.get("").resolve(saveName + "_nether");
            Path endPath = Paths.get("").resolve(saveName + "_the_end");
            return Files.exists(netherPath) || Files.exists(endPath);
        }
        return false;
    }

    private Path resolveExtractedSaveDir(Path extractPath) throws IOException {
        if (Files.exists(extractPath.resolve("level.dat"))) {
            return extractPath;
        }

        try (Stream<Path> stream = Files.list(extractPath)) {
            List<Path> candidates = stream
                    .filter(Files::isDirectory)
                    .filter(path -> Files.exists(path.resolve("level.dat")))
                    .collect(Collectors.toList());
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
        }

        return null;
    }

    private void splitBukkitDimensions(Path savePath, String saveName) throws IOException {
        Path netherDim = savePath.resolve("DIM-1");
        Path endDim = savePath.resolve("DIM1");

        if (Files.exists(netherDim)) {
            Path netherRoot = Paths.get("").resolve(saveName + "_nether");
            Files.createDirectories(netherRoot);
            Path netherTarget = netherRoot.resolve("DIM-1");
            Utils.copyDirectoryRecursively(netherDim, netherTarget);
            Utils.deleteDirectoryRecursively(netherDim);
        }

        if (Files.exists(endDim)) {
            Path endRoot = Paths.get("").resolve(saveName + "_the_end");
            Files.createDirectories(endRoot);
            Path endTarget = endRoot.resolve("DIM1");
            Utils.copyDirectoryRecursively(endDim, endTarget);
            Utils.deleteDirectoryRecursively(endDim);
        }
    }

    private void prepareForNewBackup() {
        if (config.maxBackups <= 0) {
            return;
        }

        try {
            List<BackupInfo> backups = provider.list();
            int targetKeep = Math.max(0, config.maxBackups - 1);
            if (backups.size() > targetKeep) {
                for (int i = backups.size() - 1; i >= targetKeep; i--) {
                    BackupInfo oldBackup = backups.get(i);
                    try {
                        provider.delete(oldBackup.getFileName());
                        plugin.logger.info("Deleted old backup: " + oldBackup.getFileName());
                    } catch (IOException e) {
                        plugin.logger.warn(
                                "Failed to delete old backup " + oldBackup.getFileName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            plugin.logger.warn("Failed to cleanup old backups: " + e.getMessage());
        }
    }

    /**
     * Shuts down the backup executor.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        net.opanel.utils.Utils.copyDirectoryRecursively(source, target);
    }

    private void deleteDirectory(Path dir) throws IOException {
        net.opanel.utils.Utils.deleteDirectoryRecursively(dir);
    }
}
