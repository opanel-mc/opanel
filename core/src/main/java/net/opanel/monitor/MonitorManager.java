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

import static net.opanel.monitor.Monitor.*;

public class MonitorManager {
    public static final int MAX_HISTORY_SIZE = 200;
    public static final long SNAPSHOT_INTERVAL_MS = 1000L;

    private final OPanel plugin;
    private final SystemInfo si = new SystemInfo();
    private final CpuSampler cpuSampler = new CpuSampler(si);
    private final ArrayDeque<MonitorData> monitorDataList = new ArrayDeque<>(MAX_HISTORY_SIZE);
    private final Set<Consumer<MonitorData>> updateListeners = new CopyOnWriteArraySet<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "opanel-monitor-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    public MonitorManager(OPanel plugin) {
        this.plugin = plugin;

        // Initialize the data list
        for(int i = 0; i < MAX_HISTORY_SIZE; i++) {
            monitorDataList.addLast(new MonitorData(
                    0,
                    0,
                    0,
                    20,
                    0,
                    0
            ));
        }

        // Initialize the timer
        snapshotMonitor();
        scheduler.scheduleAtFixedRate(
                this::snapshotMonitor,
                SNAPSHOT_INTERVAL_MS,
                SNAPSHOT_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void snapshotMonitor() {
        try {
            MonitorData data = new MonitorData(
                    cpuSampler.sampleRate(),
                    getMemoryRate(si),
                    getJvmMemoryRate(),
                    TPS.getRecentTPS(),
                    0, // todo
                    0 // todo
            );

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

    public void addUpdateListener(Consumer<MonitorData> listener) {
        updateListeners.add(listener);
    }

    public void removeUpdateListener(Consumer<MonitorData> listener) {
        updateListeners.remove(listener);
    }

    public void shutdown() {
        scheduler.shutdownNow();
        updateListeners.clear();
    }
}
