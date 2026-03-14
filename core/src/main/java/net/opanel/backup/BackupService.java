package net.opanel.backup;

import net.opanel.OPanel;
import net.opanel.backup.provider.BackupProvider;
import net.opanel.backup.provider.LocalBackupProvider;
import net.opanel.backup.provider.S3BackupProvider;
import net.opanel.backup.provider.WebDavBackupProvider;
import net.opanel.common.OPanelSave;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;
import net.opanel.utils.ZipUtility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class BackupService {
    private static BackupService instance;

    private final OPanel plugin;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ConcurrentHashMap<String, ReentrantLock> saveLocks = new ConcurrentHashMap<>();
    private final ExecutorService backupExecutor;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault());
    private static final String LOCAL_PROVIDER_ID = "__local__";
    private static final String LOCAL_PROVIDER_NAME = "LOCAL";
    private static final Path LOCAL_BACKUP_ROOT = OPanel.OPANEL_DIR_PATH.resolve("backups");

    private BackupService(OPanel plugin) {
        this.plugin = plugin;
        int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        backupExecutor = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "opanel-backup-worker");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public static BackupService get(OPanel plugin) {
        if(instance == null) {
            instance = new BackupService(plugin);
        }
        return instance;
    }

    public static BackupService getIfPresent() {
        return instance;
    }

    public List<BackupProviderConfig> getProviders(boolean redactSecrets) {
        readLock.lock();
        try {
            BackupProviderList providerList = getProviderListUnsafe();
            List<BackupProviderConfig> result = new ArrayList<>();
            for(BackupProviderConfig provider : providerList.providers) {
                result.add(copyProvider(provider, redactSecrets));
            }
            return result;
        } finally {
            readLock.unlock();
        }
    }

    public BackupProviderConfig createProvider(BackupProviderConfig provider) {
        if(provider == null) {
            throw new IllegalArgumentException("Provider body is missing.");
        }

        validateProvider(provider);

        writeLock.lock();
        try {
            BackupProviderList providerList = getProviderListUnsafe();
            BackupProviderConfig data = copyProvider(provider, false);
            data.id = Utils.generateRandomCharSequence(16, false);
            providerList.providers.add(data);
            saveProviderListUnsafe(providerList);
            return copyProvider(data, true);
        } finally {
            writeLock.unlock();
        }
    }

    public void updateProvider(String providerId, BackupProviderConfig provider) {
        if(provider == null) {
            throw new IllegalArgumentException("Provider body is missing.");
        }

        writeLock.lock();
        try {
            BackupProviderList providerList = getProviderListUnsafe();
            BackupProviderConfig target = findProviderUnsafe(providerList, providerId);
            if(target == null) {
                throw new NoSuchElementException("Cannot find provider: " + providerId);
            }

            BackupProviderConfig incoming = copyProvider(provider, false);
            mergeSensitiveFields(target, incoming);
            validateProvider(incoming);

            target.name = incoming.name;
            target.type = incoming.type;
            target.s3 = incoming.s3;
            target.webdav = incoming.webdav;
            saveProviderListUnsafe(providerList);
        } finally {
            writeLock.unlock();
        }
    }

    public void deleteProvider(String providerId) {
        writeLock.lock();
        try {
            BackupProviderList providerList = getProviderListUnsafe();
            BackupProviderConfig target = findProviderUnsafe(providerList, providerId);
            if(target == null) {
                throw new NoSuchElementException("Cannot find provider: " + providerId);
            }

            BackupRecordList recordList = getRecordListUnsafe();
            for(BackupRecord record : recordList.records) {
                if(record.providerId.equals(providerId)) {
                    throw new IllegalStateException("Provider is still used by backup records.");
                }
            }

            providerList.providers.remove(target);
            saveProviderListUnsafe(providerList);
        } finally {
            writeLock.unlock();
        }
    }

    public void testProvider(String providerId) throws Exception {
        BackupProviderConfig provider;
        readLock.lock();
        try {
            provider = resolveProviderByIdUnsafe(providerId);
        } finally {
            readLock.unlock();
        }

        BackupProvider backupProvider = createProviderClient(provider);
        backupProvider.testConnection();
    }

    public List<BackupRecord> getSaveBackups(String saveName) {
        validateSaveName(saveName, "saveName");

        readLock.lock();
        try {
            BackupRecordList recordList = getRecordListUnsafe();
            List<BackupRecord> result = new ArrayList<>();
            for(BackupRecord record : recordList.records) {
                if(record.saveName.equals(saveName)) {
                    result.add(copyRecord(record));
                }
            }

            result.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
            return result;
        } finally {
            readLock.unlock();
        }
    }

    public BackupRecord createBackup(String saveName, String providerId) throws Exception {
        if(providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("Provider id is missing.");
        }

        validateSaveName(saveName, "saveName");

        OPanelSave save = plugin.getServer().getSave(saveName);
        if(save == null) {
            throw new NoSuchElementException("Cannot find the specified save.");
        }

        BackupProviderConfig provider;
        BackupRecord record = new BackupRecord();
        writeLock.lock();
        try {
            if(isRunningBackupForSaveUnsafe(saveName)) {
                throw new IllegalStateException("A backup task is already running for this save.");
            }

            provider = resolveProviderByIdUnsafe(providerId);

            record.id = Utils.generateRandomCharSequence(16, false);
            record.saveName = saveName;
            record.providerId = providerId;
            record.providerName = provider.name;
            record.createdAt = System.currentTimeMillis();
            record.updatedAt = record.createdAt;
            record.status = BackupStatus.RUNNING;
            record.error = "";

            addRecordUnsafe(record);
        } finally {
            writeLock.unlock();
        }

        final String backupId = record.id;
        final BackupProviderConfig providerSnapshot = copyProvider(provider, false);
        backupExecutor.execute(() -> runBackupJob(saveName, backupId, providerSnapshot));
        return copyRecord(record);
    }

    public BackupRecord getBackup(String saveName, String backupId) {
        readLock.lock();
        try {
            BackupRecord backupRecord = getBackupRecordUnsafe(saveName, backupId);
            if(backupRecord == null) {
                throw new NoSuchElementException("Cannot find backup record: " + backupId);
            }
            return backupRecord;
        } finally {
            readLock.unlock();
        }
    }

    private void runBackupJob(String saveName, String backupId, BackupProviderConfig provider) {
        ReentrantLock saveLock = saveLocks.computeIfAbsent(saveName, key -> new ReentrantLock());
        saveLock.lock();

        Path zipPath = null;
        List<Path> copiedDimensionFolders = new ArrayList<>();

        try {
            OPanelSave save = plugin.getServer().getSave(saveName);
            if(save == null) {
                throw new NoSuchElementException("Cannot find the specified save.");
            }

            BackupRecord currentRecord = getBackup(saveName, backupId);

            if(save.isRunning()) {
                plugin.getServer().saveAll();
            }

            PreparedZip preparedZip = prepareZip(save, backupId);
            zipPath = preparedZip.zipPath;
            copiedDimensionFolders = preparedZip.copiedDimensionFolders;

            String remoteKey = generateRemoteKey(provider, saveName, backupId, currentRecord.createdAt);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("save-name", saveName);
            metadata.put("backup-id", backupId);
            metadata.put("timestamp", String.valueOf(currentRecord.createdAt));

            BackupProvider client = createProviderClient(provider);
            client.upload(zipPath, remoteKey, metadata);

            currentRecord.remoteKey = remoteKey;
            currentRecord.sizeBytes = Files.size(zipPath);
            currentRecord.sha256 = hashSha256(zipPath);
            currentRecord.status = BackupStatus.SUCCESS;
            currentRecord.error = "";
            currentRecord.updatedAt = System.currentTimeMillis();
            updateRecord(currentRecord);
        } catch (Exception e) {
            try {
                BackupRecord currentRecord = getBackup(saveName, backupId);
                currentRecord.status = BackupStatus.FAILED;
                currentRecord.error = (e.getMessage() == null || e.getMessage().isBlank())
                        ? e.toString()
                        : e.getMessage();
                currentRecord.updatedAt = System.currentTimeMillis();
                updateRecord(currentRecord);
            } catch (Exception updateErr) {
                plugin.logger.warn("Cannot update failed backup record status: " + updateErr.getMessage());
            }
            plugin.logger.warn("Backup task failed: " + e.getMessage());
        } finally {
            if(zipPath != null && Files.exists(zipPath)) {
                try {
                    Files.deleteIfExists(zipPath);
                } catch (IOException e) {
                    plugin.logger.warn("Cannot clean temp backup file: " + e.getMessage());
                }
            }
            for(Path folder : copiedDimensionFolders) {
                if(Files.exists(folder)) {
                    try {
                        Utils.deleteDirectoryRecursively(folder);
                    } catch (IOException e) {
                        plugin.logger.warn("Cannot clean copied dimension folder: " + e.getMessage());
                    }
                }
            }
            saveLock.unlock();
        }
    }

    public String restoreBackup(String saveName, String backupId, String targetSaveName) throws Exception {
        validateSaveName(saveName, "saveName");

        BackupRecord backupRecord;
        BackupProviderConfig provider;

        readLock.lock();
        try {
            backupRecord = getBackupRecordUnsafe(saveName, backupId);
            if(backupRecord == null) {
                throw new NoSuchElementException("Cannot find backup record: " + backupId);
            }
            if(backupRecord.status != BackupStatus.SUCCESS) {
                throw new IllegalStateException("Only successful backups can be restored.");
            }

            provider = resolveProviderByIdUnsafe(backupRecord.providerId);
        } finally {
            readLock.unlock();
        }

        String finalTargetSaveName = targetSaveName;
        if(finalTargetSaveName == null || finalTargetSaveName.isBlank()) {
            finalTargetSaveName = saveName + "-restore-" + TIME_FORMATTER.format(Instant.now()).replace("-", "");
        }
        validateSaveName(finalTargetSaveName, "targetSaveName");

        final Path targetPath = Paths.get("").resolve(finalTargetSaveName);
        if(Files.exists(targetPath)) {
            throw new IllegalStateException("Restore target already exists: " + finalTargetSaveName);
        }

        ReentrantLock saveLock = saveLocks.computeIfAbsent(saveName, key -> new ReentrantLock());
        saveLock.lock();

        Path zipPath = OPanel.TMP_DIR_PATH.resolve("restore-" + backupId + ".zip");

        try {
            BackupProvider client = createProviderClient(provider);
            client.download(backupRecord.remoteKey, zipPath);

            ZipUtility.unzip(zipPath, targetPath);
            normalizeUnzippedSave(targetPath);
            return finalTargetSaveName;
        } finally {
            if(Files.exists(zipPath)) {
                Files.deleteIfExists(zipPath);
            }
            saveLock.unlock();
        }
    }

    public void deleteBackup(String saveName, String backupId) throws Exception {
        validateSaveName(saveName, "saveName");

        BackupRecord backupRecord;
        BackupProviderConfig provider;

        readLock.lock();
        try {
            backupRecord = getBackupRecordUnsafe(saveName, backupId);
            if(backupRecord == null) {
                throw new NoSuchElementException("Cannot find backup record: " + backupId);
            }
            provider = resolveProviderByIdUnsafe(backupRecord.providerId);
        } finally {
            readLock.unlock();
        }

        BackupProvider client = createProviderClient(provider);
        client.delete(backupRecord.remoteKey);

        writeLock.lock();
        try {
            BackupRecordList recordList = getRecordListUnsafe();
            recordList.records.removeIf(record -> record.id.equals(backupId) && record.saveName.equals(saveName));
            saveRecordListUnsafe(recordList);
        } finally {
            writeLock.unlock();
        }
    }

    public void shutdown() {
        backupExecutor.shutdown();
        try {
            if(!backupExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                backupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private PreparedZip prepareZip(OPanelSave save, String backupId) throws Exception {
        Path zipPath = OPanel.TMP_DIR_PATH.resolve("backup-" + backupId + ".zip");
        Path savePath = save.getPath();
        List<Path> copiedDimensionFolders = new ArrayList<>();

        if(plugin.getServer().getServerType().isBukkitSeries()) {
            final String saveName = save.getName();
            Path netherDimSource = Paths.get("").resolve(saveName + "_nether/DIM-1");
            Path theEndDimSource = Paths.get("").resolve(saveName + "_the_end/DIM1");
            Path netherDimTarget = savePath.resolve("DIM-1");
            Path theEndDimTarget = savePath.resolve("DIM1");

            if(Files.exists(netherDimSource)) {
                boolean existsBefore = Files.exists(netherDimTarget);
                Utils.copyDirectoryRecursively(netherDimSource, netherDimTarget);
                if(!existsBefore) copiedDimensionFolders.add(netherDimTarget);
            }
            if(Files.exists(theEndDimSource)) {
                boolean existsBefore = Files.exists(theEndDimTarget);
                Utils.copyDirectoryRecursively(theEndDimSource, theEndDimTarget);
                if(!existsBefore) copiedDimensionFolders.add(theEndDimTarget);
            }
        }

        ZipUtility.zip(savePath, zipPath);
        return new PreparedZip(zipPath, copiedDimensionFolders);
    }

    private void normalizeUnzippedSave(Path targetPath) throws Exception {
        if(Files.exists(targetPath.resolve("level.dat"))) return;

        Path folderInside = targetPath.resolve(targetPath.getFileName()).toAbsolutePath();
        if(!Files.exists(folderInside)) {
            Utils.deleteDirectoryRecursively(targetPath);
            throw new IllegalArgumentException("Invalid backup file.");
        }

        try(Stream<Path> stream = Files.list(folderInside)) {
            stream.forEach(path -> {
                try {
                    Utils.copyDirectoryRecursively(path, targetPath.resolve(path.getFileName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if(e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }

        Utils.deleteDirectoryRecursively(folderInside);
        if(!Files.exists(targetPath.resolve("level.dat"))) {
            Utils.deleteDirectoryRecursively(targetPath);
            throw new IllegalArgumentException("Invalid backup file.");
        }
    }

    private String hashSha256(Path filePath) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        try(
                InputStream is = Files.newInputStream(filePath);
                DigestInputStream digestInputStream = new DigestInputStream(is, messageDigest)
        ) {
            byte[] bytes = new byte[8192];
            while(digestInputStream.read(bytes) != -1) {}
        }

        byte[] hash = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for(byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generateRemoteKey(BackupProviderConfig provider, String saveName, String backupId, long createdAt) {
        String time = TIME_FORMATTER.format(Instant.ofEpochMilli(createdAt));
        String normalizedSaveName = saveName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = "saves/" + normalizedSaveName + "/" + time + "-" + backupId + ".zip";

        if(provider.type != BackupProviderType.S3) {
            return key;
        }

        String prefix = provider.s3.prefix == null ? "" : provider.s3.prefix.trim();
        while(prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while(prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if(prefix.isBlank()) {
            return key;
        }
        return prefix + "/" + key;
    }

    private BackupProvider createProviderClient(BackupProviderConfig provider) {
        if(provider.type == BackupProviderType.LOCAL) {
            return new LocalBackupProvider(LOCAL_BACKUP_ROOT);
        }
        if(provider.type == BackupProviderType.S3) {
            return new S3BackupProvider(provider.s3);
        }
        if(provider.type == BackupProviderType.WEBDAV) {
            return new WebDavBackupProvider(provider.webdav);
        }
        throw new IllegalArgumentException("Unsupported provider type: " + provider.type);
    }

    private BackupProviderConfig getProviderByIdUnsafe(String providerId) {
        BackupProviderList providerList = getProviderListUnsafe();
        BackupProviderConfig provider = findProviderUnsafe(providerList, providerId);
        if(provider == null) {
            throw new NoSuchElementException("Cannot find provider: " + providerId);
        }
        return copyProvider(provider, false);
    }

    private BackupProviderConfig resolveProviderByIdUnsafe(String providerId) {
        if(LOCAL_PROVIDER_ID.equals(providerId)) {
            return createLocalProviderConfig();
        }
        return getProviderByIdUnsafe(providerId);
    }

    private BackupProviderConfig createLocalProviderConfig() {
        BackupProviderConfig provider = new BackupProviderConfig();
        provider.id = LOCAL_PROVIDER_ID;
        provider.name = LOCAL_PROVIDER_NAME;
        provider.type = BackupProviderType.LOCAL;
        return provider;
    }

    private BackupProviderConfig findProviderUnsafe(BackupProviderList providerList, String providerId) {
        for(BackupProviderConfig provider : providerList.providers) {
            if(provider.id.equals(providerId)) {
                return provider;
            }
        }
        return null;
    }

    private BackupRecord getBackupRecordUnsafe(String saveName, String backupId) {
        BackupRecordList recordList = getRecordListUnsafe();
        for(BackupRecord record : recordList.records) {
            if(record.id.equals(backupId) && record.saveName.equals(saveName)) {
                return copyRecord(record);
            }
        }
        return null;
    }

    private void addRecordUnsafe(BackupRecord record) {
        BackupRecordList recordList = getRecordListUnsafe();
        recordList.records.add(copyRecord(record));
        saveRecordListUnsafe(recordList);
    }

    private void updateRecord(BackupRecord record) {
        writeLock.lock();
        try {
            BackupRecordList recordList = getRecordListUnsafe();
            boolean updated = false;
            for(int i = 0; i < recordList.records.size(); i++) {
                BackupRecord item = recordList.records.get(i);
                if(item.id.equals(record.id)) {
                    recordList.records.set(i, copyRecord(record));
                    updated = true;
                    break;
                }
            }
            if(!updated) {
                recordList.records.add(copyRecord(record));
            }
            saveRecordListUnsafe(recordList);
        } finally {
            writeLock.unlock();
        }
    }

    private boolean isRunningBackupForSaveUnsafe(String saveName) {
        BackupRecordList recordList = getRecordListUnsafe();
        for(BackupRecord record : recordList.records) {
            if(record.saveName.equals(saveName) && record.status == BackupStatus.RUNNING) {
                return true;
            }
        }
        return false;
    }

    private BackupProviderList getProviderListUnsafe() {
        BackupProviderList providerList = Storage.get().getStoredData(StorageKey.CLOUD_BACKUP_PROVIDERS);
        return providerList == null ? new BackupProviderList() : providerList;
    }

    private BackupRecordList getRecordListUnsafe() {
        BackupRecordList recordList = Storage.get().getStoredData(StorageKey.CLOUD_BACKUP_RECORDS);
        return recordList == null ? new BackupRecordList() : recordList;
    }

    private void saveProviderListUnsafe(BackupProviderList providerList) {
        Storage.get().setStoredData(StorageKey.CLOUD_BACKUP_PROVIDERS, providerList);
    }

    private void saveRecordListUnsafe(BackupRecordList recordList) {
        Storage.get().setStoredData(StorageKey.CLOUD_BACKUP_RECORDS, recordList);
    }

    private BackupProviderConfig copyProvider(BackupProviderConfig src, boolean redactSecrets) {
        BackupProviderConfig data = new BackupProviderConfig();
        data.id = src.id;
        data.name = src.name;
        data.type = src.type;

        data.s3 = new BackupS3Config();
        if(src.s3 != null) {
            data.s3.endpoint = src.s3.endpoint;
            data.s3.region = src.s3.region;
            data.s3.bucket = src.s3.bucket;
            data.s3.prefix = src.s3.prefix;
            data.s3.forcePathStyle = src.s3.forcePathStyle;
            data.s3.accessKey = src.s3.accessKey;
            data.s3.secretKey = redactSecrets ? "******" : src.s3.secretKey;
        }

        data.webdav = new BackupWebDavConfig();
        if(src.webdav != null) {
            data.webdav.baseUrl = src.webdav.baseUrl;
            data.webdav.rootPath = src.webdav.rootPath;
            data.webdav.username = src.webdav.username;
            data.webdav.password = redactSecrets ? "******" : src.webdav.password;
        }

        return data;
    }

    private BackupRecord copyRecord(BackupRecord src) {
        BackupRecord data = new BackupRecord();
        data.id = src.id;
        data.saveName = src.saveName;
        data.providerId = src.providerId;
        data.providerName = src.providerName;
        data.remoteKey = src.remoteKey;
        data.createdAt = src.createdAt;
        data.updatedAt = src.updatedAt;
        data.sizeBytes = src.sizeBytes;
        data.sha256 = src.sha256;
        data.status = src.status;
        data.error = src.error;
        return data;
    }

    private void mergeSensitiveFields(BackupProviderConfig target, BackupProviderConfig incoming) {
        if(incoming.type == BackupProviderType.S3 && target.type == BackupProviderType.S3) {
            if(incoming.s3 == null) {
                incoming.s3 = new BackupS3Config();
            }
            incoming.s3.secretKey = resolveSensitiveValue(incoming.s3.secretKey, target.s3.secretKey);
            if(incoming.s3.accessKey == null || incoming.s3.accessKey.isBlank()) {
                incoming.s3.accessKey = target.s3.accessKey;
            }
            return;
        }

        if(incoming.type == BackupProviderType.WEBDAV && target.type == BackupProviderType.WEBDAV) {
            if(incoming.webdav == null) {
                incoming.webdav = new BackupWebDavConfig();
            }
            incoming.webdav.password = resolveSensitiveValue(incoming.webdav.password, target.webdav.password);
            if(incoming.webdav.username == null || incoming.webdav.username.isBlank()) {
                incoming.webdav.username = target.webdav.username;
            }
        }
    }

    private String resolveSensitiveValue(String value, String fallback) {
        if(value == null || value.isBlank() || value.equals("******")) {
            return fallback;
        }
        return value;
    }

    private void validateSaveName(String saveName, String fieldName) {
        if(saveName == null || saveName.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is missing.");
        }
        if(saveName.contains("/") || saveName.contains("\\") || saveName.contains("..")) {
            throw new IllegalArgumentException("Invalid " + fieldName + ".");
        }
        if(!saveName.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid " + fieldName + ".");
        }
    }

    private void validateProvider(BackupProviderConfig provider) {
        if(provider.name == null || provider.name.isBlank()) {
            throw new IllegalArgumentException("Provider name is missing.");
        }
        if(provider.type == null) {
            throw new IllegalArgumentException("Provider type is missing.");
        }

        if(provider.type == BackupProviderType.LOCAL) {
            throw new IllegalArgumentException("LOCAL provider is built-in and cannot be created.");
        }

        if(provider.type == BackupProviderType.S3) {
            if(provider.s3 == null) {
                throw new IllegalArgumentException("S3 config is missing.");
            }
            if(provider.s3.bucket == null || provider.s3.bucket.isBlank()) {
                throw new IllegalArgumentException("S3 bucket is missing.");
            }
            if(provider.s3.region == null || provider.s3.region.isBlank()) {
                provider.s3.region = "us-east-1";
            }
            if(provider.s3.accessKey == null || provider.s3.accessKey.isBlank()) {
                throw new IllegalArgumentException("S3 access key is missing.");
            }
            if(provider.s3.secretKey == null || provider.s3.secretKey.isBlank()) {
                throw new IllegalArgumentException("S3 secret key is missing.");
            }
            if(provider.s3.prefix == null) {
                provider.s3.prefix = "";
            }
            if(provider.s3.endpoint == null) {
                provider.s3.endpoint = "";
            }
            return;
        }

        if(provider.type == BackupProviderType.WEBDAV) {
            if(provider.webdav == null) {
                throw new IllegalArgumentException("WebDAV config is missing.");
            }
            if(provider.webdav.baseUrl == null || provider.webdav.baseUrl.isBlank()) {
                throw new IllegalArgumentException("WebDAV base url is missing.");
            }
            if(provider.webdav.username == null || provider.webdav.username.isBlank()) {
                throw new IllegalArgumentException("WebDAV username is missing.");
            }
            if(provider.webdav.password == null || provider.webdav.password.isBlank()) {
                throw new IllegalArgumentException("WebDAV password is missing.");
            }
            if(provider.webdav.rootPath == null) {
                provider.webdav.rootPath = "";
            }
        }
    }

    private static class PreparedZip {
        private final Path zipPath;
        private final List<Path> copiedDimensionFolders;

        private PreparedZip(Path zipPath, List<Path> copiedDimensionFolders) {
            this.zipPath = zipPath;
            this.copiedDimensionFolders = copiedDimensionFolders;
        }
    }
}
