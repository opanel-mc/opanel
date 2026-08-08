package net.opanel.exception;

import java.io.IOException;

/**
 * Signals that a marketplace install target already exists on disk under the
 * same file name (either enabled, or as a {@code .jar.disabled} file).
 */
public class MarketplaceInstallConflictException extends IOException {
    public MarketplaceInstallConflictException(String fileName) {
        super("The file already exists: " + fileName);
    }
}