package net.opanel.backup;

public class BackupProviderConfig {
    public String id;
    public String name;
    public BackupProviderType type;
    public BackupS3Config s3;
    public BackupWebDavConfig webdav;

    public BackupProviderConfig() {
        id = "";
        name = "";
        type = BackupProviderType.S3;
        s3 = new BackupS3Config();
        webdav = new BackupWebDavConfig();
    }
}
