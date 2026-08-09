package net.opanel.api.exception;

/**
 * Thrown when an operation requires a Minecraft server implementation but
 * OPanel has not attached one or the server is no longer available.
 */
public final class ServerUnavailableException extends OPanelAPIException {
    public ServerUnavailableException(String message) {
        super(message);
    }
}
