package cn.opanel.api.plugins;

import java.util.*;

/**
 * Immutable snapshot describing one installed plugin or mod file.
 *
 * <p>Metadata may be unavailable for disabled or unloaded JAR files. Optional
 * getters represent that absence explicitly. Author lists are unmodifiable and
 * icon bytes are defensively copied both when this object is created and when
 * they are read.</p>
 *
 * <p>描述一个已安装的插件或模组文件的不可变快照。</p>
 *
 * <p>对于已禁用或未加载的 JAR 文件，元数据可能不可用。可选 getter 会显式
 * 表示这种缺失。作者列表不可修改，图标字节在创建该对象时以及读取时都会
 * 进行防御性拷贝。</p>
 */
public final class PluginInfo { // 描述一个已安装的插件或模组文件的不可变快照
    private final String fileName; // 源 JAR 文件名，可能以 .disabled 结尾
    private final String name; // 插件或模组的显示名称
    private final String version; // 声明的版本，不可用时为 null
    private final String description; // 声明的描述，不可用时为 null
    private final List<String> authors; // 声明的作者列表（不可修改）
    private final String website; // 项目网站，不可用时为 null
    private final byte[] icon; // 原始图标字节，不可用时为 null
    private final long fileSize; // 源 JAR 大小（字节）
    private final boolean enabled; // 该文件是否被配置为启用
    private final boolean loaded; // 该插件/模组是否已在当前服务器进程中加载

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
    public PluginInfo( // 创建插件描述快照
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
    public String getFileName() { // 返回确切的源文件名
        return fileName;
    }

    /**
     * @return the plugin or mod display name
     */
    public String getName() { // 返回插件或模组的显示名称
        return name;
    }

    /**
     * @return the declared version, or an empty value when metadata is unavailable
     */
    public Optional<String> getVersion() { // 返回声明的版本，元数据不可用时为空值
        return Optional.ofNullable(version);
    }

    /**
     * @return the declared description, or an empty value when unavailable
     */
    public Optional<String> getDescription() { // 返回声明的描述，不可用时为空值
        return Optional.ofNullable(description);
    }

    /**
     * @return an unmodifiable list of declared author names
     */
    public List<String> getAuthors() { // 返回声明的作者名称的不可修改列表
        return authors;
    }

    /**
     * @return the declared project website, or an empty value when unavailable
     */
    public Optional<String> getWebsite() { // 返回声明的项目网站，不可用时为空值
        return Optional.ofNullable(website);
    }

    /**
     * Returns a defensive copy of the plugin icon.
     *
     * @return copied icon bytes, or an empty value when no icon is available
     */
    public Optional<byte[]> getIcon() { // 返回插件图标的防御性拷贝，无图标时为空值
        return (
            icon == null
            ? Optional.empty()
            : Optional.of(Arrays.copyOf(icon, icon.length))
        );
    }

    /**
     * @return source JAR size in bytes
     */
    public long getFileSize() { // 返回源 JAR 大小（字节）
        return fileSize;
    }

    /**
     * Indicates the desired file state. This can differ from {@link #isLoaded()}
     * when a change is waiting for a server restart.
     *
     * @return {@code true} when the plugin/mod file is enabled
     */
    public boolean isEnabled() { // 插件/模组文件是否被启用
        return enabled;
    }

    /**
     * @return {@code true} when the plugin/mod is loaded in the current process
     */
    public boolean isLoaded() { // 插件/模组是否已在当前进程中加载
        return loaded;
    }
}
