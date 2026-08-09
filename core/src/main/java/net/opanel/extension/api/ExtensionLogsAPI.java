package net.opanel.extension.api;

import net.opanel.api.logs.LogsAPI;
import net.opanel.extension.ExtensionContext;
import net.opanel.logger.Loggable;
import net.opanel.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ExtensionLogsAPI implements LogsAPI {
    private final ExtensionContext ctx;

    ExtensionLogsAPI(ExtensionContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    @Override
    public List<String> getLogFileList() {
        return ctx.call("get log file list", () -> Collections.unmodifiableList(
                new ArrayList<>(logger().getLogFileList())
        ));
    }

    @Override
    public String getLogContent(String fileName) {
        validateFileName(fileName);
        return ctx.call("get log content", () -> logger().getLogContent(fileName));
    }

    @Override
    public void deleteLog(String fileName) {
        validateFileName(fileName);
        if(fileName.endsWith(".log")) {
            throw new IllegalArgumentException("Active .log files cannot be deleted.");
        }
        ctx.run("delete log", () -> logger().deleteLog(fileName));
    }

    private Loggable logger() {
        ctx.ensureActive();
        return ctx.getPlugin().logger;
    }

    private static void validateFileName(String fileName) {
        Objects.requireNonNull(fileName, "fileName");
        if(!Utils.isSafeFileName(fileName)) {
            throw new IllegalArgumentException("Invalid log file name: " + fileName);
        }
    }
}
