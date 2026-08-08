package net.opanel.api.exception;

public abstract class OPanelAPIException extends RuntimeException {
    protected OPanelAPIException(String message) {
        super(message);
    }

    protected OPanelAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
