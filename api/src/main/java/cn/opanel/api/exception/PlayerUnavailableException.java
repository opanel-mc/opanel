package cn.opanel.api.exception;

import java.util.UUID;

/**
 * Thrown when a player handle can no longer resolve its player UUID through the
 * active server implementation.
 */
public final class PlayerUnavailableException extends OPanelAPIException {
    private final UUID uniqueId;

    /**
     * @param uniqueId UUID that could not be resolved
     * @param message failure description
     */
    public PlayerUnavailableException(UUID uniqueId, String message) {
        super(message);
        this.uniqueId = uniqueId;
    }

    /**
     * @return the unresolved player UUID
     */
    public UUID getUniqueId() {
        return uniqueId;
    }
}
