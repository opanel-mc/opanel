package net.opanel.backup;

public class BackupS3Config {
    public String endpoint;
    public String region;
    public String bucket;
    public String accessKey;
    public String secretKey;
    public String prefix;
    public boolean forcePathStyle;

    public BackupS3Config() {
        endpoint = "";
        region = "us-east-1";
        bucket = "";
        accessKey = "";
        secretKey = "";
        prefix = "opanel";
        forcePathStyle = true;
    }
}
