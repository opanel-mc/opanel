package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.backup.BackupConfiguration;
import net.opanel.backup.BackupInfo;
import net.opanel.backup.BackupManager;
import net.opanel.backup.BackupResult;
import net.opanel.controller.BaseController;
import net.opanel.utils.CryptoUtils;

import java.io.FileNotFoundException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Controller for backup-related API endpoints.
 */
public class BackupController extends BaseController {

    public BackupController(OPanel plugin) {
        super(plugin);
    }

    /**
     * GET /api/backup/config
     * Returns the current backup configuration.
     */
    public Handler getConfig = ctx -> {
        BackupManager manager = plugin.getBackupManager();
        if (manager == null) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Backup manager not initialized");
            return;
        }

        BackupConfiguration config = manager.getConfig();
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("config", configToMap(config));
        sendResponse(ctx, obj);
    };

    /**
     * POST /api/backup/config
     * Updates the backup configuration.
     */
    public Handler setConfig = ctx -> {
        BackupManager manager = plugin.getBackupManager();
        if (manager == null) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Backup manager not initialized");
            return;
        }

        try {
            BackupConfigRequest req = ctx.bodyAsClass(BackupConfigRequest.class);
            BackupConfiguration config = manager.getConfig();

            // Update configuration
            config.enabled = req.enabled;
            config.providerType = req.providerType;
            config.maxBackups = req.maxBackups;

            // Local settings
            config.localPath = req.localPath != null ? req.localPath : "";

            String salt = plugin.getConfig().salt;

            // S3 settings
            config.s3Endpoint = req.s3Endpoint != null ? req.s3Endpoint : "";
            config.s3AccessKeyEncrypted = CryptoUtils.encrypt(req.s3AccessKey != null ? req.s3AccessKey : "", salt);
            if (req.s3SecretKey != null && "********".equals(req.s3SecretKey)) {
                // Keep existing secret key
            } else {
                config.s3SecretKeyEncrypted = CryptoUtils.encrypt(req.s3SecretKey != null ? req.s3SecretKey : "", salt);
            }
            config.s3Bucket = req.s3Bucket != null ? req.s3Bucket : "";
            config.s3Region = req.s3Region != null ? req.s3Region : "";
            config.s3Prefix = req.s3Prefix != null ? req.s3Prefix : "";

            // WebDAV settings
            config.webdavUrl = req.webdavUrl != null ? req.webdavUrl : "";
            config.webdavUsernameEncrypted = CryptoUtils.encrypt(req.webdavUsername != null ? req.webdavUsername : "", salt);
            if (req.webdavPassword != null && "********".equals(req.webdavPassword)) {
                // Keep existing password
            } else {
                config.webdavPasswordEncrypted = CryptoUtils.encrypt(req.webdavPassword != null ? req.webdavPassword : "", salt);
            }

            manager.setConfig(config);
            sendResponse(ctx, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    /**
     * POST /api/backup/trigger
     * Triggers a new backup.
     */
    public Handler triggerBackup = ctx -> {
        BackupManager manager = plugin.getBackupManager();
        if (manager == null) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Backup manager not initialized");
            return;
        }

        if (!manager.isConfigured()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Backup is not configured");
            return;
        }

        try {
            // Trigger backup asynchronously
            BackupResult result = manager.performBackupAsync(() -> {
                server.saveAll();
            }).get(); // Wait for completion

            if (result.isSuccess()) {
                HashMap<String, Object> obj = new HashMap<>();
                obj.put("backup", backupInfoToMap(result.getBackupInfo()));
                sendResponse(ctx, obj);
            } else {
                sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, result.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    /**
     * GET /api/backup/list
     * Returns a list of all backups.
     */
    public Handler listBackups = ctx -> {
        BackupManager manager = plugin.getBackupManager();
        if (manager == null) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Backup manager not initialized");
            return;
        }

        if (!manager.isConfigured()) {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("backups", new ArrayList<>());
            obj.put("configured", false);
            sendResponse(ctx, obj);
            return;
        }

        try {
            List<BackupInfo> backups = manager.listBackupsAsync().get();
            List<HashMap<String, Object>> backupList = new ArrayList<>();
            for (BackupInfo info : backups) {
                backupList.add(backupInfoToMap(info));
            }

            HashMap<String, Object> obj = new HashMap<>();
            obj.put("backups", backupList);
            obj.put("configured", true);
            sendResponse(ctx, obj);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    /**
     * DELETE /api/backup/{fileName}
     * Deletes a specific backup.
     */
    public Handler deleteBackup = ctx -> {
        String fileName = ctx.pathParam("fileName");
        BackupManager manager = plugin.getBackupManager();

        if (manager == null) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Backup manager not initialized");
            return;
        }

        if (!manager.isConfigured()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Backup is not configured");
            return;
        }

        try {
            boolean success = manager.deleteBackupAsync(fileName).get();
            if (success) {
                sendResponse(ctx, HttpStatus.OK);
            } else {
                sendResponse(ctx, HttpStatus.NOT_FOUND, "Backup not found: " + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    /**
     * POST /api/backup/restore/{fileName}
     * Restores a specific backup (placeholder for future implementation).
     */
    public Handler restoreBackup = ctx -> {
        String fileName = ctx.pathParam("fileName");
        BackupManager manager = plugin.getBackupManager();
        if (manager == null) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Backup manager not initialized");
            return;
        }

        if (!manager.isConfigured()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Backup is not configured");
            return;
        }

        try {
            String restoredName = manager.restoreBackupAsync(fileName).get();
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("saveName", restoredName);
            sendResponse(ctx, obj);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException && cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause instanceof FileNotFoundException) {
                sendResponse(ctx, HttpStatus.NOT_FOUND, "Backup not found: " + fileName);
                return;
            }

            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    private HashMap<String, Object> configToMap(BackupConfiguration config) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("enabled", config.enabled);
        map.put("providerType", config.providerType);
        map.put("maxBackups", config.maxBackups);
        map.put("localPath", config.localPath);
        map.put("s3Endpoint", config.s3Endpoint);
        map.put("s3AccessKey", decryptIfNeeded(config.s3AccessKeyEncrypted));
        // Don't send actual secret key, just indicate if it's set
        map.put("s3SecretKey", config.s3SecretKeyEncrypted != null && !config.s3SecretKeyEncrypted.isEmpty() ? "********" : "");
        map.put("s3Bucket", config.s3Bucket);
        map.put("s3Region", config.s3Region);
        map.put("s3Prefix", config.s3Prefix);
        map.put("webdavUrl", config.webdavUrl);
        map.put("webdavUsername", decryptIfNeeded(config.webdavUsernameEncrypted));
        // Don't send actual password, just indicate if it's set
        map.put("webdavPassword", config.webdavPasswordEncrypted != null && !config.webdavPasswordEncrypted.isEmpty() ? "********" : "");
        return map;
    }

    private HashMap<String, Object> backupInfoToMap(BackupInfo info) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("fileName", info.getFileName());
        map.put("saveName", info.getSaveName());
        map.put("timestamp", toEpochMillis(info.getTimestamp()));
        map.put("fileSize", info.getSize());
        return map;
    }

    private long toEpochMillis(java.time.LocalDateTime timestamp) {
        if (timestamp == null) {
            return 0L;
        }
        return timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String decryptIfNeeded(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (CryptoUtils.isEncrypted(value)) {
            try {
                return CryptoUtils.decrypt(value, plugin.getConfig().salt);
            } catch (RuntimeException e) {
                return "";
            }
        }
        return value;
    }

    private static class BackupConfigRequest {
        boolean enabled;
        String providerType;
        int maxBackups;
        String localPath;
        String s3Endpoint;
        String s3AccessKey;
        String s3SecretKey;
        String s3Bucket;
        String s3Region;
        String s3Prefix;
        String webdavUrl;
        String webdavUsername;
        String webdavPassword;
    }
}
