package net.opanel.update;

import java.io.IOException;

/**
 * Signals that a plugin file changed after its update information was fetched.
 */
public class PluginUpdateConflictException extends IOException {
    public PluginUpdateConflictException(String fileName) {
        super("Plugin file changed after the update check: " + fileName);
    }
}
