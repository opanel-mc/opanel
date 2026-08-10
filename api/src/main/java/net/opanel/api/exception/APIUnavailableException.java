package net.opanel.api.exception;

/**
 * Thrown when an extension uses an API handle after that extension was unloaded.
 *
 * <p>Every child API obtained from the root OPanel API shares the root handle's
 * lifetime and becomes unavailable at the same time.</p>
 */
public final class APIUnavailableException extends OPanelAPIException {
    public APIUnavailableException(String message) {
        super(message);
    }
}
