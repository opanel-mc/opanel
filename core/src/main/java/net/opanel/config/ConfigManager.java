package net.opanel.config;

public interface ConfigManager {
    OPanelConfiguration get();
    void set(OPanelConfiguration config);
}
