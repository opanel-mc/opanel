package net.opanel.backup;

public class BackupWebDavConfig {
    public String baseUrl;
    public String username;
    public String password;
    public String rootPath;

    public BackupWebDavConfig() {
        baseUrl = "";
        username = "";
        password = "";
        rootPath = "opanel";
    }
}
