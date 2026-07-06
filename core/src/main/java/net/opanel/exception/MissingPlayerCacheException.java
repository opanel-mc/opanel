package net.opanel.exception;

/**
 * {@link MissingPlayerCacheException} is thrown when a player can't be
 * found in the server's usercache.json
 */
public class MissingPlayerCacheException extends RuntimeException {
    public MissingPlayerCacheException(String message) {
        super(message);
    }
}
