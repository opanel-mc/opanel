package net.opanel.backup;

/**
 * Configuration for the backup system.
 * Sensitive fields (access keys, passwords) are stored encrypted using AES.
 */
public class BackupConfiguration {
    public static final BackupConfiguration defaultConfig = new BackupConfiguration();

    /**
     * Whether backup is enabled.
     */
    public boolean enabled = false;

    /**
     * The type of backup provider to use: LOCAL, S3, or WEBDAV.
     */
    public String providerType = "LOCAL";

    /**
     * Maximum number of backups to keep. Older backups will be deleted
     * automatically.
     */
    public int maxBackups = 5;

    // ========== Local Configuration ==========

    /**
     * Path to the local backup directory (relative to server root).
     */
    public String localPath = "opanel/backups";

    // ========== S3 Configuration ==========

    /**
     * S3 endpoint URL (e.g., "https://s3.amazonaws.com" or custom endpoint for
     * MinIO/OSS).
     */
    public String s3Endpoint = "";

    /**
     * S3 access key (AES encrypted).
     */
    public String s3AccessKeyEncrypted = "";

    /**
     * S3 secret key (AES encrypted).
     */
    public String s3SecretKeyEncrypted = "";

    /**
     * S3 bucket name.
     */
    public String s3Bucket = "";

    /**
     * S3 region (e.g., "us-east-1").
     */
    public String s3Region = "us-east-1";

    /**
     * Optional prefix/folder path in the bucket.
     */
    public String s3Prefix = "";

    // ========== WebDAV Configuration ==========

    /**
     * WebDAV server URL (e.g., "https://dav.example.com/backups/").
     */
    public String webdavUrl = "";

    /**
     * WebDAV username (AES encrypted).
     */
    public String webdavUsernameEncrypted = "";

    /**
     * WebDAV password (AES encrypted).
     */
    public String webdavPasswordEncrypted = "";

    public BackupConfiguration() {
    }

    /**
     * Gets the provider type as an enum.
     */
    public BackupProviderType getProviderTypeEnum() {
        try {
            return BackupProviderType.valueOf(providerType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BackupProviderType.LOCAL;
        }
    }
}
