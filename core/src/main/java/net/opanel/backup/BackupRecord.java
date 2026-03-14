package net.opanel.backup;

public class BackupRecord {
    public String id;
    public String saveName;
    public String providerId;
    public String providerName;
    public String remoteKey;
    public long createdAt;
    public long updatedAt;
    public long sizeBytes;
    public String sha256;
    public BackupStatus status;
    public String error;

    public BackupRecord() {
        id = "";
        saveName = "";
        providerId = "";
        providerName = "";
        remoteKey = "";
        createdAt = 0;
        updatedAt = 0;
        sizeBytes = 0;
        sha256 = "";
        status = BackupStatus.RUNNING;
        error = "";
    }
}
