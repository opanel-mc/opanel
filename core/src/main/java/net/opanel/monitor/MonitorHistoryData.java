package net.opanel.monitor;

import java.util.Objects;

public record MonitorHistoryData(
        long timestamp,
        long durationMs,
        long sampleCount,
        MonitorData average,
        MonitorData minimum,
        MonitorData maximum
) {
    public MonitorHistoryData {
        if(durationMs <= 0) throw new IllegalArgumentException("durationMs must be positive");
        if(sampleCount <= 0) throw new IllegalArgumentException("sampleCount must be positive");
        Objects.requireNonNull(average, "average");
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
    }
}
