package cn.opanel.api.exception;

import java.util.Objects;

/**
 * Wraps an unexpected platform, I/O, validation, or implementation failure that
 * occurred while executing a named extension API operation.
 *
 * <p>The original exception is available through {@link #getCause()}.</p>
 */
public final class OperationFailedException extends OPanelAPIException {
    private final String operation;

    /**
     * Creates a wrapped operation failure.
     *
     * @param operation short operation description, such as {@code get player}
     * @param cause underlying failure
     * @throws NullPointerException if an argument is {@code null}
     */
    public OperationFailedException(String operation, Throwable cause) {
        super("Failed to " + Objects.requireNonNull(operation, "operation") + ".", Objects.requireNonNull(cause, "cause"));
        this.operation = operation;
    }

    /**
     * @return the operation description supplied when the exception was created
     */
    public String getOperation() {
        return operation;
    }
}
