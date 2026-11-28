package cn.opanel.api.plugins;

import cn.opanel.api.exception.ActLaterException;
import cn.opanel.api.exception.OperationFailedException;

import java.util.List;
import java.util.Optional;

/**
 * Read and management access to server plugins or mods.
 *
 * <p>The term "plugin" in this API covers Bukkit/Paper plugins as well as
 * Fabric, Forge, and NeoForge mods. Returned descriptors and lists are
 * immutable snapshots. File-management operations only accept a safe,
 * single-segment {@code .jar} or {@code .jar.disabled} file name.</p>
 *
 * <p>对服务器插件或模组的读取与管理访问。</p>
 *
 * <p>本 API 中的“插件”一词涵盖 Bukkit/Paper 插件，以及 Fabric、Forge 和
 * NeoForge 模组。返回的描述符与列表都是不可变快照。文件管理操作只接受
 * 安全的、单段的 {@code .jar} 或 {@code .jar.disabled} 文件名。</p>
 */
public interface PluginsAPI { // 对服务器插件或模组的读取与管理访问
    /**
     * Lists plugins and mods discovered by the server implementation.
     *
     * @return an unmodifiable snapshot list; never {@code null}
     */
    List<PluginInfo> getPlugins(); // 列出服务器实现发现的插件与模组

    /**
     * Finds a plugin or mod by its exact source file name.
     *
     * @param fileName safe {@code .jar} or {@code .jar.disabled} file name
     * @return the matching descriptor, or an empty value when no file matches
     * @throws NullPointerException if {@code fileName} is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is unsafe or has an
     *         unsupported suffix
     */
    Optional<PluginInfo> getPlugin(String fileName); // 按确切的源文件名查找插件或模组

    /**
     * Changes whether a plugin or mod is enabled. This operation may block and
     * must not be called from an extension lifecycle callback or the Minecraft
     * main thread.
     *
     * <p>Some platforms cannot apply this operation to a plugin that is already
     * loaded. In that case the file change is accepted for the next server start
     * and {@link ActLaterException} is thrown to distinguish deferred success
     * from an immediate change.</p>
     *
     * @param fileName safe {@code .jar} or {@code .jar.disabled} file name
     * @param enabled {@code true} to enable the file, {@code false} to disable it
     * @throws NullPointerException if {@code fileName} is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is invalid
     * @throws ActLaterException if the accepted change requires a server restart
     * @throws OperationFailedException if the file does
     *         not exist or the platform rejects the operation
     */
    void setEnabled(String fileName, boolean enabled) throws ActLaterException; // 更改插件或模组是否启用

    /**
     * Deletes a plugin or mod file. This operation may block and must not be
     * called from an extension lifecycle callback or the Minecraft main thread.
     *
     * <p>Some platforms cannot delete a plugin that is already loaded. In that
     * case the deletion is accepted for the next server start and
     * {@link ActLaterException} is thrown to distinguish deferred success from
     * an immediate deletion.</p>
     *
     * @param fileName safe {@code .jar} or {@code .jar.disabled} file name
     * @throws NullPointerException if {@code fileName} is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is invalid
     * @throws ActLaterException if the accepted deletion requires a server restart
     * @throws OperationFailedException if the file does not exist or the
     *         platform rejects the operation
     */
    void deletePlugin(String fileName) throws ActLaterException; // 删除插件或模组文件
}
