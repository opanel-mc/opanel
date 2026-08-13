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
 */
public interface PluginsAPI {
    /**
     * Lists plugins and mods discovered by the server implementation.
     *
     * @return an unmodifiable snapshot list; never {@code null}
     */
    List<PluginInfo> getPlugins();

    /**
     * Finds a plugin or mod by its exact source file name.
     *
     * @param fileName safe {@code .jar} or {@code .jar.disabled} file name
     * @return the matching descriptor, or an empty value when no file matches
     * @throws NullPointerException if {@code fileName} is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is unsafe or has an
     *         unsupported suffix
     */
    Optional<PluginInfo> getPlugin(String fileName);

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
    void setEnabled(String fileName, boolean enabled) throws ActLaterException;
}
