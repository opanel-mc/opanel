package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HandlerType;

public interface OPanelAPI {
    String getOPanelVersion();
    ServerAPI getServer();
    void logInfo(String message);
    void logWarn(String message);
    void logError(String message);
    void addHandler(String path, HandlerType method, Handler handler);
}
