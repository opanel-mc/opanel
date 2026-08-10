package cn.opanel.api.monitor;

import java.util.List;

/**
 * Read-only access to host and Minecraft server performance samples.
 *
 * <p>Each result is an immutable snapshot. History lists are ordered from the
 * oldest retained sample to the newest and cannot be modified.</p>
 */
public interface MonitorAPI {
    /**
     * Samples current CPU, memory, TPS, and network usage immediately.
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
}
