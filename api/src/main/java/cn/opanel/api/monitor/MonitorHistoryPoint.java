package cn.opanel.api.monitor;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable aggregate of monitor samples for one aligned time bucket.
 *
 * <p>单个对齐时间桶内监控采样的不可变聚合。</p>
 */
public final class MonitorHistoryPoint { // 监控历史聚合点：单个对齐时间桶内采样的不可变聚合
    private final Instant startTime; // 时间桶的起始时间
    private final Duration duration; // 时间桶的时长
    private final long sampleCount; // 该时间桶内的采样数量
    private final MonitorSnapshot average; // 平均值快照
    private final MonitorSnapshot minimum; // 最小值快照
    private final MonitorSnapshot maximum; // 最大值快照

    public MonitorHistoryPoint( // 构造聚合点，参数均不允许为 null，且 duration 必须为正、sampleCount 不得为负
            Instant startTime,
            Duration duration,
            long sampleCount,
            MonitorSnapshot average,
            MonitorSnapshot minimum,
            MonitorSnapshot maximum
    ) {
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.duration = Objects.requireNonNull(duration, "duration");
        this.average = Objects.requireNonNull(average, "average");
        this.minimum = Objects.requireNonNull(minimum, "minimum");
        this.maximum = Objects.requireNonNull(maximum, "maximum");
        if(duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if(sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount cannot be negative");
        }
        this.sampleCount = sampleCount;
    }

    public Instant getStartTime() { // 返回时间桶的起始时间
        return startTime;
    }

    public Duration getDuration() { // 返回时间桶的时长
        return duration;
    }

    public long getSampleCount() { // 返回该时间桶内的采样数量
        return sampleCount;
    }

    public MonitorSnapshot getAverage() { // 返回平均值快照
        return average;
    }

    public MonitorSnapshot getMinimum() { // 返回最小值快照
        return minimum;
    }

    public MonitorSnapshot getMaximum() { // 返回最大值快照
        return maximum;
    }
}
