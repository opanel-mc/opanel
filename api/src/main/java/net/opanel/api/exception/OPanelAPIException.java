package net.opanel.api.exception;

/**
 * Base runtime exception for failures reported by the OPanel extension API.
 *
 * <p>Callers may catch this type for general API failures or catch a concrete
 * subtype when recovery depends on the failure category.</p>
 */
public abstract class OPanelAPIException extends RuntimeException {
    /**
     * Creates an API exception with a descriptive message.
     *
     * @param message failure description
     */
    protected OPanelAPIException(String message) {
        super(message);
    }

    /**
     * Creates an API exception with a descriptive message and original cause.
     *
     * @param message failure description
     * @param cause underlying failure
     */
    protected OPanelAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
