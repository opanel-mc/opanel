package net.opanel.extension.api;

import cn.opanel.api.monitor.MonitorAPI;
import cn.opanel.api.monitor.MonitorSnapshot;
import net.opanel.extension.ExtensionContext;
import net.opanel.monitor.MonitorData;
import net.opanel.monitor.MonitorManager;

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
                data.cpu,
                data.memory,
                data.jvmMemory,
                data.tps,
                data.networkUpload,
                data.networkDownload
        );
    }
}
