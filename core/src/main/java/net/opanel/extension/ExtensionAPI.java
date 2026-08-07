package net.opanel.extension;

import net.opanel.OPanel;
import net.opanel.api.OPanelAPI;

public class ExtensionAPI implements OPanelAPI {
    private final OPanel plugin;
    private final String extensionId;

    public ExtensionAPI(OPanel plugin, String extensionId) {
        this.plugin = plugin;
        this.extensionId = extensionId;
    }

    @Override
    public String getOPanelVersion() {
        return OPanel.VERSION;
    }

    @Override
    public void logInfo(String message) {
        plugin.logger.info(formatLog(message));
    }

    @Override
    public void logWarn(String message) {
        plugin.logger.warn(formatLog(message));
    }

    @Override
    public void logError(String message) {
        plugin.logger.error(formatLog(message));
    }

    private String formatLog(String message) {
        return "[" + extensionId + "] " + message;
    }
}
