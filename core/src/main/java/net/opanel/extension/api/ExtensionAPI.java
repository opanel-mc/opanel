package net.opanel.extension.api;

import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import net.opanel.OPanel;
import net.opanel.api.OPanelAPI;
import net.opanel.api.ServerAPI;
import net.opanel.extension.ExtensionContext;
import net.opanel.extension.LoadedExtension;
import net.opanel.utils.Utils;

public final class ExtensionAPI implements OPanelAPI {
    private final ExtensionContext ctx;
    private final ServerAPI server;

    public ExtensionAPI(ExtensionContext ctx) {
        this.ctx = ctx;
        this.server = new ExtensionServerAPI(ctx);
    }

    public void invalidate() {
        ctx.invalidate();
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
