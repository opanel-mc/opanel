package net.opanel.backup;

/**
 * Result of a backup operation.
 */
public class BackupResult {
    private final boolean success;
    private final String message;
    private final BackupInfo backupInfo;

    private BackupResult(boolean success, String message, BackupInfo backupInfo) {
        this.success = success;
        this.message = message;
        this.backupInfo = backupInfo;
    }

    /**
     * Creates a successful backup result.
     */
    public static BackupResult success(String message, BackupInfo backupInfo) {
        return new BackupResult(true, message, backupInfo);
    }

    /**
     * Creates a failed backup result.
     */
    public static BackupResult failure(String message) {
        return new BackupResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public BackupInfo getBackupInfo() {
        return backupInfo;
    }
}
