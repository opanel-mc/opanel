package net.opanel.api.exception;

import java.util.UUID;

/**
 * Thrown when a player exists but its current state does not permit an operation,
 * for example when attempting to kick an offline player.
 */
public final class InvalidPlayerStateException extends OPanelAPIException {
    private final UUID uniqueId;

    /**
     * @param uniqueId UUID of the player in the invalid state
     * @param message failure description
     */
    public InvalidPlayerStateException(UUID uniqueId, String message) {
        super(message);
        this.uniqueId = uniqueId;
    }

    /**
     * @return UUID of the affected player
     */
    public UUID getUniqueId() {
        return uniqueId;
    }
}
