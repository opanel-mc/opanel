package net.opanel.monitor;

import java.util.List;

public record MonitorHistoryQueryResult(
        long from,
        long to,
        long resolutionMs,
        List<MonitorHistoryData> points
) {
    public MonitorHistoryQueryResult {
        points = List.copyOf(points);
    }
}
