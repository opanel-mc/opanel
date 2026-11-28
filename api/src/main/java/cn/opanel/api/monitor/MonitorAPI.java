package cn.opanel.api.monitor;

import java.time.Instant;
import java.util.List;

/**
 * Read-only access to host and Minecraft server performance samples.
 *
 * <p>Each result is an immutable snapshot. History lists are ordered from the
 * oldest retained sample to the newest and cannot be modified.</p>
 *
 * <p>只读访问主机与 Minecraft 服务器的性能采样数据。</p>
 *
 * <p>每个结果都是不可变的快照。历史列表按从最旧保留样本到最新样本的顺序排列，
 * 且不可修改。</p>
 */
public interface MonitorAPI { // 主机与 Minecraft 服务器监控 API
    /**
     * Samples current CPU, memory, TPS, network usage, and disk I/O immediately.
     *
     * @return a current performance snapshot
     */
    MonitorSnapshot getSnapshot(); // 立即采样当前的 CPU、内存、TPS、网络使用率与磁盘 I/O

    /**
     * Returns all retained periodic samples up to OPanel's history capacity.
     *
     * @return an unmodifiable chronological history list
     */
    List<MonitorSnapshot> getHistory(); // 返回 OPanel 历史容量内的所有保留周期采样

    /**
     * Returns at most the newest {@code limit} retained samples. Negative limits
     * produce an empty list, and values above the history capacity are clamped.
     *
     * @param limit maximum number of newest samples to return (less than or equal to 200)
     * @return an unmodifiable chronological history list
     */
    List<MonitorSnapshot> getHistory(int limit); // 返回最多最新 limit 个保留采样；负数返回空列表，超出容量会被截断

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
    default List<MonitorHistoryPoint> queryHistory(Instant from, Instant to, int maxPoints) { // 查询半开区间 [from, to) 内持久化的聚合历史
        throw new IllegalStateException("Persistent monitor history is not supported.");
    }
}
