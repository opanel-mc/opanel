package net.opanel.exception;

/**
 * Signals that the upstream Modrinth API returned HTTP 429 (Too Many Requests).
 *
 * <p>Extends {@link RuntimeException} rather than {@link java.io.IOException} so
 * that it is not silently swallowed by the update provider's
 * {@code catch (IOException)} blocks, but is instead propagated to the
 * MarketplaceController which converts it to an HTTP 429 response.</p>
 */
public class MarketplaceRateLimitException extends RuntimeException {
    public MarketplaceRateLimitException() {
        super("The upstream API returned HTTP 429 (rate limited). Please try again later.");
    }
}