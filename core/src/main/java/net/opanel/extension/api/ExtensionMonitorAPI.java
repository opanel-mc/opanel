package net.opanel.extension.api;

import cn.opanel.api.monitor.MonitorAPI;
import cn.opanel.api.monitor.MonitorHistoryPoint;
import cn.opanel.api.monitor.MonitorSnapshot;
import net.opanel.extension.ExtensionContext;
import net.opanel.monitor.MonitorData;
import net.opanel.monitor.MonitorManager;
import net.opanel.monitor.MonitorHistoryData;
import net.opanel.monitor.MonitorHistoryQueryResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ExtensionMonitorAPI implements MonitorAPI {
    private final ExtensionContext ctx;

    ExtensionMonitorAPI(ExtensionContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    @Override
    public MonitorSnapshot getSnapshot() {
        return ctx.call("get monitor snapshot", () -> toSnapshot(manager().getSnapshot()));
    }

    @Override
    public List<MonitorSnapshot> getHistory() {
        return ctx.call("get monitor history", () -> toSnapshots(manager().getHistory()));
    }

    @Override
    public List<MonitorSnapshot> getHistory(int limit) {
        return ctx.call("get monitor history", () -> toSnapshots(manager().getHistory(limit)));
    }

    @Override
    public List<MonitorHistoryPoint> queryHistory(Instant from, Instant to, int maxPoints) {
        ctx.ensureActive();
        if(from == null || to == null) throw new IllegalArgumentException("from and to are required.");

        final long fromMillis;
        final long toMillis;
        try {
            fromMillis = from.toEpochMilli();
            toMillis = to.toEpochMilli();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Monitor history range is outside epoch millisecond limits.", e);
        }

        MonitorHistoryQueryResult result = manager().queryHistory(fromMillis, toMillis, maxPoints);
        List<MonitorHistoryPoint> points = new ArrayList<>(result.points().size());
        for(MonitorHistoryData point : result.points()) {
            points.add(new MonitorHistoryPoint(
                    Instant.ofEpochMilli(point.timestamp()),
                    Duration.ofMillis(point.durationMs()),
                    point.sampleCount(),
                    toSnapshot(point.average()),
                    toSnapshot(point.minimum()),
                    toSnapshot(point.maximum())
            ));
        }
        return Collections.unmodifiableList(points);
    }

    private MonitorManager manager() {
        ctx.ensureActive();
        return ctx.getPlugin().getMonitorManager();
    }

    private static List<MonitorSnapshot> toSnapshots(List<MonitorData> dataList) {
        List<MonitorSnapshot> snapshots = new ArrayList<>(dataList.size());
        for(MonitorData data : dataList) {
            snapshots.add(toSnapshot(data));
        }
        return Collections.unmodifiableList(snapshots);
    }

    private static MonitorSnapshot toSnapshot(MonitorData data) {
        return new MonitorSnapshot(
                data.cpu(),
                data.memory(),
                data.jvmMemory(),
                data.tps(),
                data.networkUpload(),
                data.networkDownload(),
                data.diskRead(),
                data.diskWrite()
        );
    }
}
