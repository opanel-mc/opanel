package cn.opanel.api.monitor;

import java.time.Instant;
import java.util.List;

/**
 * Read-only access to host and Minecraft server performance samples.
 *
 * <p>Each result is an immutable snapshot. History lists are ordered from the
 * oldest retained sample to the newest and cannot be modified.</p>
 */
public interface MonitorAPI {
    /**
     * Samples current CPU, memory, TPS, network usage, and disk I/O immediately.
     *
     * @return a current performance snapshot
     */
    MonitorSnapshot getSnapshot();

    /**
     * Returns all retained periodic samples up to OPanel's history capacity.
     *
     * @return an unmodifiable chronological history list
     */
    List<MonitorSnapshot> getHistory();

    /**
     * Returns at most the newest {@code limit} retained samples. Negative limits
     * produce an empty list, and values above the history capacity are clamped.
     *
     * @param limit maximum number of newest samples to return (less than or equal to 200)
     * @return an unmodifiable chronological history list
     */
    List<MonitorSnapshot> getHistory(int limit);

    /**
     * Queries persisted, aggregated history in the half-open interval
     * {@code [from, to)}. The returned points are chronological and immutable.
     *
     * @param from inclusive start time
     * @param to exclusive end time
     * @param maxPoints maximum number of returned points (between 1 and 2000)
     * @return an unmodifiable list of aggregate history points
     * @throws IllegalArgumentException if the range or point limit is invalid
     * @throws IllegalStateException if persistent monitor history is unavailable
     */
    default List<MonitorHistoryPoint> queryHistory(Instant from, Instant to, int maxPoints) {
        throw new IllegalStateException("Persistent monitor history is not supported.");
    }
}
