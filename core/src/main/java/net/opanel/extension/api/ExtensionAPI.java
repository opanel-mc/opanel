package net.opanel.extension.api;

import net.opanel.OPanel;
import net.opanel.api.OPanelAPI;
import net.opanel.api.ServerAPI;
import net.opanel.extension.ExtensionContext;

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
}
