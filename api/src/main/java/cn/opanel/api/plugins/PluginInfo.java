package cn.opanel.api.plugins;

import java.util.*;

/**
 * Immutable snapshot describing one installed plugin or mod file.
 *
 * <p>Metadata may be unavailable for disabled or unloaded JAR files. Optional
 * getters represent that absence explicitly. Author lists are unmodifiable and
 * icon bytes are defensively copied both when this object is created and when
 * they are read.</p>
 */
public final class PluginInfo {
    private final String fileName;
    private final String name;
    private final String version;
    private final String description;
    private final List<String> authors;
    private final String website;
    private final byte[] icon;
    private final long fileSize;
    private final boolean enabled;
    private final boolean loaded;

    /**
     * Creates a plugin descriptor snapshot.
     *
     * @param fileName source JAR file name, possibly ending in {@code .disabled}
     * @param name plugin or mod display name
     * @param version declared version, or {@code null} when unavailable
     * @param description declared description, or {@code null} when unavailable
     * @param authors declared authors; {@code null} is treated as an empty list
     * @param website project website, or {@code null} when unavailable
     * @param icon raw icon bytes, or {@code null} when unavailable
     * @param fileSize source JAR size in bytes
     * @param enabled whether the file is configured to be enabled
     * @param loaded whether the plugin/mod is loaded in the current server process
     * @throws NullPointerException if {@code fileName}, {@code name}, or an
     *         author entry is {@code null}
     */
    public PluginInfo(
            String fileName,
            String name,
            String version,
            String description,
            List<String> authors,
            String website,
            byte[] icon,
            long fileSize,
            boolean enabled,
            boolean loaded
    ) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.name = Objects.requireNonNull(name, "name");
        this.version = version;
        this.description = description;
        this.authors = List.copyOf(authors == null ? Collections.emptyList() : authors);
        this.website = website;
        this.icon = icon == null ? null : Arrays.copyOf(icon, icon.length);
        this.fileSize = fileSize;
        this.enabled = enabled;
        this.loaded = loaded;
    }

    /**
     * @return the exact source file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the plugin or mod display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the declared version, or an empty value when metadata is unavailable
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * @return the declared description, or an empty value when unavailable
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * @return an unmodifiable list of declared author names
     */
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * @return the declared project website, or an empty value when unavailable
     */
    public Optional<String> getWebsite() {
        return Optional.ofNullable(website);
    }

    /**
     * Returns a defensive copy of the plugin icon.
     *
     * @return copied icon bytes, or an empty value when no icon is available
     */
    public Optional<byte[]> getIcon() {
        return (
            icon == null
            ? Optional.empty()
            : Optional.of(Arrays.copyOf(icon, icon.length))
        );
    }

    /**
     * @return source JAR size in bytes
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Indicates the desired file state. This can differ from {@link #isLoaded()}
     * when a change is waiting for a server restart.
     *
     * @return {@code true} when the plugin/mod file is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return {@code true} when the plugin/mod is loaded in the current process
     */
    public boolean isLoaded() {
        return loaded;
    }
}
