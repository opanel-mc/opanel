package cn.opanel.api;

import cn.opanel.api.exception.APIUnavailableException;
import cn.opanel.api.logs.LogsAPI;
import cn.opanel.api.monitor.MonitorAPI;
import cn.opanel.api.plugins.PluginsAPI;
import cn.opanel.api.server.ServerAPI;
import cn.opanel.api.tasks.TasksAPI;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;

/**
 * Root entry point exposed to an OPanel extension.
 *
 * <p>The same instance is supplied to the extension's {@link ExtensionLoad}
 * callback and remains valid until that extension is unloaded. API objects
 * returned by the accessor methods below are stable handles and share the same
 * lifetime. Calling any of them after unload results in an API-unavailable
 * {@link APIUnavailableException}.</p>
 *
 * <p>Unless a method explicitly returns a mutable object, values returned by
 * this API are snapshots and cannot be used to mutate OPanel state directly.</p>
 *
 * <p>暴露给 OPanel 扩展的根入口点。</p>
 *
 * <p>同一个实例会传递给扩展的 {@link ExtensionLoad} 回调，并一直有效直到该扩展
 * 被卸载。下方访问器方法返回的 API 对象是稳定的句柄，具有相同的生命周期。
 * 在卸载之后调用其中任何一个方法都会抛出 API 不可用的
 * {@link APIUnavailableException}。</p>
 *
 * <p>除非某个方法明确返回可变对象，否则本 API 返回的值都是快照，
 * 不能用于直接修改 OPanel 的状态。</p>
 */
public interface OPanelAPI { // OPanel 扩展根 API 接口
    /**
     * Returns the version of the running OPanel instance.
     *
     * @return the OPanel version string
     */
    String getOPanelVersion(); // 返回当前运行的 OPanel 实例的版本号

    /**
     * Returns the server-management API.
     *
     * @return a stable server API handle
     */
    ServerAPI getServer(); // 返回服务器管理 API

    /**
     * Returns the installed plugin and mod management API.
     *
     * @return a stable plugins API handle
     */
    PluginsAPI getPluginsAPI(); // 返回已安装的插件与模组管理 API

    /**
     * Returns the server log-file management API.
     *
     * @return a stable logs API handle
     */
    LogsAPI getLogsAPI(); // 返回服务器日志文件管理 API

    /**
     * Returns the scheduled-task management API.
     *
     * @return a stable tasks API handle
     */
    TasksAPI getTasksAPI(); // 返回定时任务管理 API

    /**
     * Returns the host and Minecraft server monitoring API.
     *
     * @return a stable monitor API handle
     */
    MonitorAPI getMonitor(); // 返回主机与 Minecraft 服务器监控 API

    /**
     * Writes an informational message using OPanel's logger. OPanel prefixes the
     * message with the extension's display name.
     *
     * @param message message to write
     */
    void logInfo(String message); // 使用 OPanel 日志器写入信息级别日志（自动附加扩展显示名前缀）

    /**
     * Writes a warning message using OPanel's logger. OPanel prefixes the message
     * with the extension's display name.
     *
     * @param message message to write
     */
    void logWarn(String message); // 使用 OPanel 日志器写入警告级别日志（自动附加扩展显示名前缀）

    /**
     * Writes an error message using OPanel's logger. OPanel prefixes the message
     * with the extension's display name.
     *
     * @param message message to write
     */
    void logError(String message); // 使用 OPanel 日志器写入错误级别日志（自动附加扩展显示名前缀）

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
    void addHandler(String path, HandlerType method, Handler handler); // 在扩展的后端命名空间中注册 HTTP 处理器
}
