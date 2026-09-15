package cn.opanel.api.monitor;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable aggregate of monitor samples for one aligned time bucket.
 */
public final class MonitorHistoryPoint {
    private final Instant startTime;
    private final Duration duration;
    private final long sampleCount;
    private final MonitorSnapshot average;
    private final MonitorSnapshot minimum;
    private final MonitorSnapshot maximum;

    public MonitorHistoryPoint(
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

    public Instant getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public MonitorSnapshot getAverage() {
        return average;
    }

    public MonitorSnapshot getMinimum() {
        return minimum;
    }

    public MonitorSnapshot getMaximum() {
        return maximum;
    }
}
