package net.opanel.api;

public interface OPanelAPI {
    String getOPanelVersion();
    ServerAPI getServer();
    void logInfo(String message);
    void logWarn(String message);
    void logError(String message);
}
