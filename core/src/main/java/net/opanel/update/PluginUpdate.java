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
    private final String source;
    private final String projectId;
    private final boolean requiresBinding;
    private final boolean requiresRestart;
    private final String channel;
    private final String digestAlgorithm;
    private final String digestValue;

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
        this(fileName, name, currentVersion, latestVersion, downloadUrl, projectUrl, fileSha1, installedFileSha1,
            null, null, false, false, null, null, null);
    }

    public PluginUpdate(
        String fileName,
        String name,
        String currentVersion,
        String latestVersion,
        String downloadUrl,
        String projectUrl,
        String fileSha1,
        String installedFileSha1,
        String source,
        String projectId,
        boolean requiresBinding,
        boolean requiresRestart,
        String channel,
        String digestAlgorithm,
        String digestValue
    ) {
        this.fileName = fileName;
        this.name = name;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
        this.projectUrl = projectUrl;
        this.fileSha1 = fileSha1;
        this.installedFileSha1 = installedFileSha1;
        this.source = source;
        this.projectId = projectId;
        this.requiresBinding = requiresBinding;
        this.requiresRestart = requiresRestart;
        this.channel = channel;
        this.digestAlgorithm = digestAlgorithm;
        this.digestValue = digestValue;
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

    public String getSource() {
        return source;
    }

    public String getProjectId() {
        return projectId;
    }

    public boolean isRequiresBinding() {
        return requiresBinding;
    }

    public boolean isRequiresRestart() {
        return requiresRestart;
    }

    public String getChannel() {
        return channel;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public String getDigestValue() {
        return digestValue;
    }

    public boolean isAutoApplySafe() {
        return digestValue != null && !digestValue.isEmpty();
    }
}
