package net.opanel.api;

public interface OPanelAPI {
    String getOPanelVersion();
    void logInfo(String message);
    void logWarn(String message);
    void logError(String message);
}
