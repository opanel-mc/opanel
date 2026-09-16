package net.opanel.monitor;

import net.opanel.OPanel;
import net.opanel.config.MonitorHistoryConfiguration;
import net.opanel.logger.Loggable;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;

public final class MonitorHistoryManager {
    public static final int DEFAULT_MAX_POINTS = 500;
    public static final int MAX_POINTS = 2000;

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final long MAINTENANCE_INTERVAL_MILLIS = 60L * 60 * 1000;
    private static final long RETRY_INTERVAL_MINUTES = 5;
    private static final long QUERY_TIMEOUT_SECONDS = 10;

    private final Loggable logger;
    private final MonitorHistoryConfiguration config;
    private final Clock clock;
    private final MonitorHistoryStore store;
    private final MonitorHistoryAccumulator accumulator = new MonitorHistoryAccumulator();
    private final ScheduledExecutorService executor;

    private volatile boolean available;
    private volatile boolean shutdown;
    private volatile boolean invalidSampleWarningLogged;
    private long lastMaintenance;

    public MonitorHistoryManager(OPanel plugin) {
        this(
                plugin.logger,
                MonitorHistoryConfiguration.from(plugin.getConfig(), plugin.logger),
                OPanel.OPANEL_DIR_PATH.resolve("monitor-history"),
                Clock.systemUTC()
        );
    }

    MonitorHistoryManager(
            Loggable logger,
            MonitorHistoryConfiguration config,
            Path databaseBasePath,
            Clock clock
    ) {
        this.logger = logger;
        this.config = config;
        this.clock = clock;
        this.store = new MonitorHistoryStore(databaseBasePath);

        if(!config.enabled()) {
            executor = null;
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "opanel-monitor-history");
            thread.setDaemon(true);
            return thread;
        });
        openStore(false);
        executor.scheduleWithFixedDelay(
                this::backgroundMaintenance,
                RETRY_INTERVAL_MINUTES,
                RETRY_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public boolean isAvailable() {
        return config.enabled() && available && !shutdown;
    }

    public void recordSample(long timestamp, MonitorData sample) {
        if(!config.enabled() || shutdown) return;
        if(!MonitorAggregate.isFinite(sample)) {
            if(!invalidSampleWarningLogged) {
                invalidSampleWarningLogged = true;
                logger.warn("Skipped a monitor history sample containing a non-finite value.");
            }
            return;
        }

        MonitorAggregate closed;
        synchronized(accumulator) {
            closed = accumulator.add(timestamp, sample);
        }
        if(closed != null) submitPersist(closed, timestamp);
    }

    public MonitorHistoryQueryResult queryHistory(long from, long to, int maxPoints) {
        validateQuery(from, to, maxPoints);
        if(!isAvailable()) throw new IllegalStateException("Persistent monitor history is unavailable.");

        final Future<MonitorHistoryQueryResult> future;
        try {
            future = executor.submit(() -> queryOnExecutor(from, to, maxPoints));
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException("Persistent monitor history is unavailable.", e);
        }

        try {
            return future.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Monitor history query was interrupted.", e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("Monitor history query timed out.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if(cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Failed to query monitor history.", cause);
        }
    }

    public void shutdown() {
        if(shutdown) return;
        shutdown = true;
        if(executor == null) return;

        MonitorAggregate partial;
        synchronized(accumulator) {
            partial = accumulator.drain();
        }

        try {
            Future<?> closeFuture = executor.submit(() -> {
                if(partial != null && available) {
                    try {
                        store.persistMinute(partial, clock.millis());
                    } catch (SQLException e) {
                        handleStoreFailure("flush the final monitor history bucket", e);
                    }
                }
                closeStoreQuietly();
            });
            closeFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException | RejectedExecutionException e) {
            logger.warn("Failed to close monitor history cleanly: " + e.getMessage());
            closeStoreQuietly();
        } finally {
            executor.shutdownNow();
        }
    }

    private void submitPersist(MonitorAggregate aggregate, long completedBefore) {
        if(!available) return;
        try {
            executor.execute(() -> {
                if(!available) return;
                try {
                    store.persistMinute(aggregate, completedBefore);
                } catch (SQLException e) {
                    handleStoreFailure("persist monitor history", e);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Shutdown won the race; the in-memory realtime monitor remains available.
        }
    }

    private MonitorHistoryQueryResult queryOnExecutor(long from, long to, int maxPoints) {
        if(!available || shutdown) {
            throw new IllegalStateException("Persistent monitor history is unavailable.");
        }

        try {
            long now = clock.millis();
            int sourceResolution = selectSourceResolution(from, now);
            long outputResolution = selectOutputResolution(from, to, sourceResolution, maxPoints);
            List<MonitorAggregate> source = readStitchedRange(sourceResolution, from, to, now);
            Map<Long, MonitorAggregate> grouped = new TreeMap<>();
            Map<Long, Long> groupEnds = new TreeMap<>();
            for(MonitorAggregate aggregate : source) {
                long outputBucketStart = MonitorHistoryAccumulator.align(
                        aggregate.bucketStart(),
                        outputResolution
                );
                long aggregateEnd = Math.addExact(aggregate.bucketStart(), aggregate.resolutionMillis());
                groupEnds.merge(outputBucketStart, aggregateEnd, Math::max);
                grouped.compute(outputBucketStart, (k, existing) -> (
                        existing == null
                        ? new MonitorAggregate(
                            sourceResolution,
                            outputBucketStart,
                            aggregate.sampleCount(),
                            aggregate.sum(),
                            aggregate.minimum(),
                            aggregate.maximum()
                        )
                        : existing.merge(aggregate, sourceResolution, outputBucketStart)
                ));
            }

            List<MonitorHistoryData> points = new ArrayList<>(grouped.size());
            for(Map.Entry<Long, MonitorAggregate> entry : grouped.entrySet()) {
                MonitorAggregate aggregate = entry.getValue();
                long duration = Math.min(outputResolution, groupEnds.get(entry.getKey()) - entry.getKey());
                points.add(new MonitorHistoryData(
                        entry.getKey(),
                        duration,
                        aggregate.sampleCount(),
                        aggregate.average(),
                        aggregate.minimum(),
                        aggregate.maximum()
                ));
            }
            return new MonitorHistoryQueryResult(from, to, outputResolution, points);
        } catch (SQLException e) {
            handleStoreFailure("query monitor history", e);
            throw new IllegalStateException("Persistent monitor history is unavailable.", e);
        }
    }

    private List<MonitorAggregate> readStitchedRange(
            int sourceResolution,
            long from,
            long to,
            long now
    ) throws SQLException {
        List<MonitorAggregate> aggregates = new ArrayList<>();
        long cursor = from;
        long availableEnd = Math.min(to, now);

        if(sourceResolution == MonitorHistoryStore.HOURLY_RESOLUTION_SECONDS) {
            long completedHoursEnd = MonitorHistoryAccumulator.align(
                    availableEnd,
                    MonitorHistoryStore.HOURLY_RESOLUTION_SECONDS * 1000L
            );
            cursor = appendRange(
                    aggregates,
                    MonitorHistoryStore.HOURLY_RESOLUTION_SECONDS,
                    cursor,
                    Math.min(to, completedHoursEnd)
            );
        }

        if(sourceResolution != MonitorHistoryStore.MINUTE_RESOLUTION_SECONDS) {
            long completedQuarterHoursEnd = MonitorHistoryAccumulator.align(
                    availableEnd,
                    MonitorHistoryStore.QUARTER_HOUR_RESOLUTION_SECONDS * 1000L
            );
            cursor = appendRange(
                    aggregates,
                    MonitorHistoryStore.QUARTER_HOUR_RESOLUTION_SECONDS,
                    cursor,
                    Math.min(to, completedQuarterHoursEnd)
            );
        }

        appendRange(
                aggregates,
                MonitorHistoryStore.MINUTE_RESOLUTION_SECONDS,
                cursor,
                availableEnd
        );
        return aggregates;
    }

    private long appendRange(
            List<MonitorAggregate> destination,
            int resolutionSeconds,
            long from,
            long to
    ) throws SQLException {
        if(from >= to) return from;
        destination.addAll(store.readRange(resolutionSeconds, from, to));
        return to;
    }

    private int selectSourceResolution(long from, long now) {
        if(from >= retentionCutoff(now, config.minuteRetentionDays())) {
            return MonitorHistoryStore.MINUTE_RESOLUTION_SECONDS;
        }
        if(from >= retentionCutoff(now, config.quarterHourRetentionDays())) {
            return MonitorHistoryStore.QUARTER_HOUR_RESOLUTION_SECONDS;
        }
        return MonitorHistoryStore.HOURLY_RESOLUTION_SECONDS;
    }

    private long selectOutputResolution(long from, long to, int sourceResolutionSeconds, int maxPoints) {
        long sourceResolutionMillis = sourceResolutionSeconds * 1000L;
        long span = to - from;
        long desiredResolution = ceilDiv(span, maxPoints);
        long factor = ceilDiv(desiredResolution, sourceResolutionMillis);
        if(factor > Long.MAX_VALUE / sourceResolutionMillis) {
            throw new IllegalArgumentException("Requested monitor history range is too large.");
        }

        long outputResolution = factor * sourceResolutionMillis;
        try {
            while(alignedBucketCount(from, to, outputResolution) > maxPoints) {
                outputResolution = Math.addExact(outputResolution, sourceResolutionMillis);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Requested monitor history range is too large.", e);
        }
        return outputResolution;
    }

    private long alignedBucketCount(long from, long to, long resolution) {
        return Math.floorDiv(to - 1, resolution) - Math.floorDiv(from, resolution) + 1;
    }

    private long ceilDiv(long value, long divisor) {
        return ((value - 1) / divisor) + 1;
    }

    private void validateQuery(long from, long to, int maxPoints) {
        if(from >= to) throw new IllegalArgumentException("from must be earlier than to.");
        try {
            Math.subtractExact(to, from);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Requested monitor history range is too large.", e);
        }
        if(maxPoints < 1 || maxPoints > MAX_POINTS) {
            throw new IllegalArgumentException("maxPoints must be between 1 and " + MAX_POINTS + ".");
        }
    }

    private void backgroundMaintenance() {
        if(shutdown) return;
        if(!available) {
            openStore(true);
            return;
        }

        long now = clock.millis();
        if(now - lastMaintenance < MAINTENANCE_INTERVAL_MILLIS) return;
        try {
            store.maintain(now, config);
            lastMaintenance = now;
        } catch (SQLException e) {
            handleStoreFailure("maintain monitor history", e);
        }
    }

    private void openStore(boolean recovery) {
        try {
            store.open();
            long now = clock.millis();
            store.maintain(now, config);
            lastMaintenance = now;
            available = true;
            if(recovery) logger.info("Monitor history storage recovered.");
        } catch (SQLException e) {
            available = false;
            closeStoreQuietly();
            logger.warn("Monitor history storage is unavailable: " + e.getMessage());
        }
    }

    private void handleStoreFailure(String operation, SQLException error) {
        available = false;
        closeStoreQuietly();
        logger.warn("Failed to " + operation + ": " + error.getMessage());
    }

    private void closeStoreQuietly() {
        try {
            store.close();
        } catch (SQLException e) {
            logger.warn("Failed to close monitor history storage: " + e.getMessage());
        }
    }

    private long retentionCutoff(long now, int retentionDays) {
        return now - retentionDays * DAY_MILLIS;
    }
}
