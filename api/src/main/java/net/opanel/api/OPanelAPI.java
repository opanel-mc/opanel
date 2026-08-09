package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import net.opanel.api.logs.LogsAPI;
import net.opanel.api.monitor.MonitorAPI;
import net.opanel.api.plugins.PluginsAPI;
import net.opanel.api.server.ServerAPI;
import net.opanel.api.tasks.TasksAPI;

/**
 * Root entry point exposed to an OPanel extension.
 *
 * <p>The same instance is supplied to the extension's {@link ExtensionLoad}
 * callback and remains valid until that extension is unloaded. API objects
 * returned by the accessor methods below are stable handles and share the same
 * lifetime. Calling any of them after unload results in an API-unavailable
 * {@link net.opanel.api.exception.APIUnavailableException}.</p>
 *
 * <p>Unless a method explicitly returns a mutable object, values returned by
 * this API are snapshots and cannot be used to mutate OPanel state directly.</p>
 */
public interface OPanelAPI {
    /**
     * Returns the version of the running OPanel instance.
     *
     * @return the OPanel version string
     */
    String getOPanelVersion();

    /**
     * Returns the server-management API.
     *
     * @return a stable server API handle
     */
    ServerAPI getServer();

    /**
     * Returns the installed plugin and mod management API.
     *
     * @return a stable plugins API handle
     */
    PluginsAPI getPluginsAPI();

    /**
     * Returns the server log-file management API.
     *
     * @return a stable logs API handle
     */
    LogsAPI getLogsAPI();

    /**
     * Returns the scheduled-task management API.
     *
     * @return a stable tasks API handle
     */
    TasksAPI getTasksAPI();

    /**
     * Returns the host and Minecraft server monitoring API.
     *
     * @return a stable monitor API handle
     */
    MonitorAPI getMonitor();

    /**
     * Writes an informational message using OPanel's logger. OPanel prefixes the
     * message with the extension's display name.
     *
     * @param message message to write
     */
    void logInfo(String message);

    /**
     * Writes a warning message using OPanel's logger. OPanel prefixes the message
     * with the extension's display name.
     *
     * @param message message to write
     */
    void logWarn(String message);

    /**
     * Writes an error message using OPanel's logger. OPanel prefixes the message
     * with the extension's display name.
     *
     * @param message message to write
     */
    void logError(String message);

    /**
     * Registers an HTTP handler in the extension's backend namespace.
     *
     * <p>A path such as {@code status} is exposed at
     * {@code /api/extension/{extensionId}/status}. A leading slash is optional;
     * parent-directory segments and backslashes are rejected. Registering the
     * same normalized path again replaces the previous handler for that path.</p>
     *
     * @param path relative route path inside the extension namespace
     * @param method HTTP method accepted by the route
     * @param handler Javalin request handler
     */
    void addHandler(String path, HandlerType method, Handler handler);
}
