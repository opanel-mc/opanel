package net.opanel.update;

import java.util.Collections;
import java.util.List;

public class PluginUpdateBinding {
    private String fileName;
    private String source;
    private String projectId;
    private String owner;
    private String repo;
    private String assetPattern;
    private List<String> channels;

    public PluginUpdateBinding() {
    }

    public PluginUpdateBinding(
        String fileName,
        String source,
        String projectId,
        String owner,
        String repo,
        String assetPattern,
        List<String> channels
    ) {
        this.fileName = fileName;
        this.source = source;
        this.projectId = projectId;
        this.owner = owner;
        this.repo = repo;
        this.assetPattern = assetPattern;
        this.channels = channels == null || channels.isEmpty() ? Collections.singletonList("release") : channels;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getAssetPattern() {
        return assetPattern;
    }

    public void setAssetPattern(String assetPattern) {
        this.assetPattern = assetPattern;
    }

    public List<String> getChannels() {
        return channels == null || channels.isEmpty() ? Collections.singletonList("release") : channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels == null || channels.isEmpty() ? Collections.singletonList("release") : channels;
    }
}
