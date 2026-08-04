package net.opanel.update;

public class PluginUpdate {
    private final String fileName;
    private final String name;
    private final String currentVersion;
    private final String latestVersion;
    private final String downloadUrl;
    private final String projectUrl;
    private final String fileSha1;
    private final String installedFileSha1;

    public PluginUpdate(
        String fileName,
        String name,
        String currentVersion,
        String latestVersion,
        String downloadUrl,
        String projectUrl,
        String fileSha1,
        String installedFileSha1
    ) {
        this.fileName = fileName;
        this.name = name;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
        this.projectUrl = projectUrl;
        this.fileSha1 = fileSha1;
        this.installedFileSha1 = installedFileSha1;
    }

    public String getFileName() {
        return fileName;
    }

    public String getName() {
        return name;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public String getFileSha1() {
        return fileSha1;
    }

    public String getInstalledFileSha1() {
        return installedFileSha1;
    }
}
