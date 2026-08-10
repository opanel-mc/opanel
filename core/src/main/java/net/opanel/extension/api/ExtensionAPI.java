package net.opanel.extension.api;

import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import net.opanel.OPanel;
import net.opanel.api.OPanelAPI;
import net.opanel.api.logs.LogsAPI;
import net.opanel.api.monitor.MonitorAPI;
import net.opanel.api.player.PlayerAPI;
import net.opanel.api.plugins.PluginsAPI;
import net.opanel.api.server.ServerAPI;
import net.opanel.api.tasks.TasksAPI;
import net.opanel.extension.ExtensionContext;
import net.opanel.extension.LoadedExtension;
import net.opanel.utils.Utils;

import java.util.Objects;
import java.util.UUID;

public final class ExtensionAPI implements OPanelAPI {
    private final ExtensionContext ctx;
    private final ServerAPI server;
    private final PluginsAPI plugins;
    private final LogsAPI logs;
    private final TasksAPI tasks;
    private final MonitorAPI monitor;

    public ExtensionAPI(ExtensionContext ctx) {
        this.ctx = ctx;

        server = new ExtensionServerAPI(ctx);
        plugins = new ExtensionPluginsAPI(ctx);
        logs = new ExtensionLogsAPI(ctx);
        tasks = new ExtensionTasksAPI(ctx);
        monitor = new ExtensionMonitorAPI(ctx);
    }

    public void invalidate() {
        ctx.invalidate();
    }

    public PlayerAPI createPlayerHandle(String uuid) {
        ctx.ensureActive();
        return new ExtensionPlayerAPI(ctx, UUID.fromString(uuid));
    }

    @Override
    public String getOPanelVersion() {
        ctx.ensureActive();
        return OPanel.VERSION;
    }

    @Override
    public ServerAPI getServer() {
        ctx.ensureActive();
        return server;
    }

    @Override
    public PluginsAPI getPluginsAPI() {
        ctx.ensureActive();
        return plugins;
    }

    @Override
    public LogsAPI getLogsAPI() {
        ctx.ensureActive();
        return logs;
    }

    @Override
    public TasksAPI getTasksAPI() {
        ctx.ensureActive();
        return tasks;
    }

    @Override
    public MonitorAPI getMonitor() {
        ctx.ensureActive();
        return monitor;
    }

    @Override
    public void logInfo(String message) {
        ctx.call("log an info message", () -> {
            ctx.getPlugin().logger.info(ctx.getLogPrefix() + message);
            return null;
        });
    }

    @Override
    public void logWarn(String message) {
        ctx.call("log a warning message", () -> {
            ctx.getPlugin().logger.warn(ctx.getLogPrefix() + message);
            return null;
        });
    }

    @Override
    public void logError(String message) {
        ctx.call("log an error message", () -> {
            ctx.getPlugin().logger.error(ctx.getLogPrefix() + message);
            return null;
        });
    }

    @Override
    public void addHandler(String path, HandlerType method, Handler handler) {
        ctx.call("register a backend route handler", () -> {
            String normalizedPath = Utils.normalizePath(path);
            if(normalizedPath == null) {
                throw new Exception("Invalid route path.");
            }

            String extensionId = ctx.getExtensionId();
            LoadedExtension extension = ctx.getPlugin().getExtensionManager().getExtension(extensionId);
            extension.addHandler(normalizedPath, method, handler);
            return null;
        });
    }
}
