package net.opanel.api.exception;

import java.util.Objects;

public final class OperationFailedException extends OPanelAPIException {
    private final String operation;

    public OperationFailedException(String operation, Throwable cause) {
        super("Failed to " + Objects.requireNonNull(operation, "operation") + ".", Objects.requireNonNull(cause, "cause"));
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
