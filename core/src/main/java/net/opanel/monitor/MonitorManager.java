package net.opanel.monitor;

import net.opanel.OPanel;
import net.opanel.time.TPS;
import oshi.SystemInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static net.opanel.monitor.Monitor.getJvmMemoryRate;
import static net.opanel.monitor.Monitor.getMemoryRate;

public class MonitorManager {
    public static final int MAX_HISTORY_SIZE = 200;

    private final PerformanceIncidentStore incidentStore = new PerformanceIncidentStore(OPanel.OPANEL_DIR_PATH.resolve("performance-incidents.json"));
    private final OPanel plugin;
    private final SystemInfo si = new SystemInfo();
    private final CpuSampler cpuSampler = new CpuSampler(si);
    private final NetworkMonitor networkMonitor = new NetworkMonitor(si);
    private final DiskMonitor diskMonitor = new DiskMonitor(si);
    private final MonitorHistoryManager historyManager;

    private final ArrayDeque<MonitorData> monitorDataList = new ArrayDeque<>(MAX_HISTORY_SIZE);
    private final Set<Consumer<MonitorData>> updateListeners = new CopyOnWriteArraySet<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final long snapshotIntervalMs;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "opanel-monitor-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    public MonitorManager(OPanel plugin) {
        this.plugin = plugin;
        snapshotIntervalMs = plugin.getConfig().monitorSnapshotInterval;
        historyManager = new MonitorHistoryManager(plugin);

        var config = plugin.getConfig();
        try {
            PerformanceTickSampler.configure(config.diagnosticsEnabled && config.performanceIncidentsEnabled,
                    config.performanceIncidentThresholdMs, config.performanceIncidentTriggerSeconds,
                    config.performanceIncidentRecoverySeconds);
        } catch(IllegalArgumentException e) {
            plugin.logger.warn("Invalid performance incident thresholds; using 50 ms / 10 s / 5 s.");
            PerformanceTickSampler.configure(config.diagnosticsEnabled && config.performanceIncidentsEnabled, 50, 10, 5);
        }

        if(PerformanceTickSampler.enabled()) {
            try { incidentStore.load(); } catch(Exception e) { plugin.logger.warn("Cannot load performance incidents: " + e.getMessage()); }
        }

        // Initialize the data list
        for(int i = 0; i < MAX_HISTORY_SIZE; i++) {
            monitorDataList.addLast(new MonitorData(
                    0,
                    0,
                    0,
                    20,
                    0,
                    0,
                    0,
                    0
            ));
        }

        // Initialize the timer
        snapshotMonitor();
        scheduler.scheduleAtFixedRate(
                this::snapshotMonitor,
                snapshotIntervalMs,
                snapshotIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void snapshotMonitor() {
        try {
            NetworkMonitor.NetworkRate networkRate = networkMonitor.sampleRate();
            DiskMonitor.DiskRate diskRate = diskMonitor.sampleRate();
            MonitorData data = new MonitorData(
                    cpuSampler.sampleRate(),
                    getMemoryRate(si),
                    getJvmMemoryRate(),
                    TPS.getRecentTPS(),
                    networkRate.uploadRate(),
                    networkRate.downloadRate(),
                    diskRate.readRate(),
                    diskRate.writeRate()
            );
            historyManager.recordSample(System.currentTimeMillis(), data);
            try { incidentStore.save(false); } catch(Exception e) { plugin.logger.warn("Cannot save performance incidents: " + e.getMessage()); }

            lock.writeLock().lock();
            try {
                if(monitorDataList.size() >= MAX_HISTORY_SIZE) {
                    monitorDataList.removeFirst();
                }
                monitorDataList.addLast(data);
            } finally {
                lock.writeLock().unlock();
            }

            for(Consumer<MonitorData> listener : updateListeners) {
                try {
                    listener.accept(data);
                } catch (Exception e) {
                    plugin.logger.warn("Failed to dispatch monitor update: " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            plugin.logger.warn("Failed to snapshot monitor data: " + e.getMessage());
        }
    }

    public MonitorData getSnapshot() {
        NetworkMonitor.NetworkRate networkRate = networkMonitor.sampleRate();
        DiskMonitor.DiskRate diskRate = diskMonitor.sampleRate();
        return new MonitorData(
                cpuSampler.sampleRate(),
                getMemoryRate(si),
                getJvmMemoryRate(),
                TPS.getRecentTPS(),
                networkRate.uploadRate(),
                networkRate.downloadRate(),
                diskRate.readRate(),
                diskRate.writeRate()
        );
    }

    public List<MonitorData> getHistory() {
        return getHistory(MAX_HISTORY_SIZE);
    }

    public List<MonitorData> getHistory(int limit) {
        int safeLimit = Math.max(0, Math.min(limit, MAX_HISTORY_SIZE));

        lock.readLock().lock();
        try {
            List<MonitorData> history = new ArrayList<>(monitorDataList);
            if(safeLimit >= history.size()) {
                return history;
            }
            return new ArrayList<>(history.subList(history.size() - safeLimit, history.size()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public MonitorHistoryQueryResult queryHistory(long from, long to, int maxPoints) {
        return historyManager.queryHistory(from, to, maxPoints);
    }

    public boolean isHistoryAvailable() {
        return historyManager.isAvailable();
    }

    public void addUpdateListener(Consumer<MonitorData> listener) {
        updateListeners.add(listener);
    }

    public void removeUpdateListener(Consumer<MonitorData> listener) {
        updateListeners.remove(listener);
    }

    public void shutdown() {
        scheduler.shutdownNow();
        try {
            if(!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.logger.warn("Monitor scheduler did not stop cleanly.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try { incidentStore.save(true); } catch(Exception e) { plugin.logger.warn("Cannot save performance incidents: " + e.getMessage()); }
        historyManager.shutdown();
        updateListeners.clear();
    }
}
