package net.opanel.api.exception;

/**
 * Signals deferred success: an operation was accepted but will be applied later,
 * normally during the next server restart.
 *
 * <p>This is distinct from {@link OperationFailedException}; callers should not
 * retry the operation immediately. It is currently used by plugin/mod state
 * changes on platforms that cannot safely change a loaded JAR in place.</p>
 */
public class ActLaterException extends RuntimeException {
    public ActLaterException() {
        super();
    }
}
