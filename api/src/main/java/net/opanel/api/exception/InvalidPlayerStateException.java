package net.opanel.api.exception;

import java.util.UUID;

public final class InvalidPlayerStateException extends OPanelAPIException {
    private final UUID uniqueId;

    public InvalidPlayerStateException(UUID uniqueId, String message) {
        super(message);
        this.uniqueId = uniqueId;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }
}
