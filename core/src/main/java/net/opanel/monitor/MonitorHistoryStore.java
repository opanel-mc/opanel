package net.opanel.monitor;

import net.opanel.config.MonitorHistoryConfiguration;
import org.h2.Driver;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

final class MonitorHistoryStore implements AutoCloseable {
    static final int MINUTE_RESOLUTION_SECONDS = 60;
    static final int QUARTER_HOUR_RESOLUTION_SECONDS = 15 * 60;
    static final int HOURLY_RESOLUTION_SECONDS = 60 * 60;

    private static final int SCHEMA_VERSION = 1;
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final String AGGREGATE_COLUMNS = """
            resolution_seconds, bucket_start, sample_count,
            cpu_sum, cpu_min, cpu_max,
            memory_sum, memory_min, memory_max,
            jvm_memory_sum, jvm_memory_min, jvm_memory_max,
            tps_sum, tps_min, tps_max,
            network_upload_sum, network_upload_min, network_upload_max,
            network_download_sum, network_download_min, network_download_max,
            disk_read_sum, disk_read_min, disk_read_max,
            disk_write_sum, disk_write_min, disk_write_max
            """;
    private static final String UPSERT_SQL = "MERGE INTO monitor_aggregate ("
            + AGGREGATE_COLUMNS
            + ") KEY (resolution_seconds, bucket_start) VALUES ("
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
            + ")";

    private final Path databaseBasePath;
    private Connection connection;

    MonitorHistoryStore(Path databaseBasePath) {
        this.databaseBasePath = databaseBasePath.toAbsolutePath().normalize();
    }

    void open() throws SQLException {
        close();

        Properties properties = new Properties();
        properties.setProperty("user", "sa");
        properties.setProperty("password", "");
        String databasePath = databaseBasePath.toString().replace('\\', '/');
        connection = new Driver().connect(
                "jdbc:h2:file:" + databasePath + ";DB_CLOSE_ON_EXIT=FALSE",
                properties
        );
        if(connection == null) throw new SQLException("H2 rejected the monitor history database URL.");

        try {
            initializeSchema();
        } catch (SQLException e) {
            close();
            throw e;
        }
    }

    boolean isOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    void persistMinute(MonitorAggregate aggregate, long completedBefore) throws SQLException {
        if(aggregate.resolutionSeconds() != MINUTE_RESOLUTION_SECONDS) {
            throw new IllegalArgumentException("Only minute aggregates can be persisted directly.");
        }

        transaction(() -> {
            MonitorAggregate existing = readAggregate(aggregate.resolutionSeconds(), aggregate.bucketStart());
            MonitorAggregate merged = existing == null
                    ? aggregate
                    : existing.merge(aggregate, MINUTE_RESOLUTION_SECONDS, aggregate.bucketStart());
            upsertAggregate(merged);
            rollUpAfterMinute(aggregate.bucketStart(), completedBefore);
        });
    }

    void maintain(long now, MonitorHistoryConfiguration config) throws SQLException {
        transaction(() -> {
            rebuildCompleted(MINUTE_RESOLUTION_SECONDS, QUARTER_HOUR_RESOLUTION_SECONDS, now);
            rebuildCompleted(QUARTER_HOUR_RESOLUTION_SECONDS, HOURLY_RESOLUTION_SECONDS, now);

            deleteBefore(MINUTE_RESOLUTION_SECONDS, retentionCutoff(now, config.minuteRetentionDays()));
            deleteBefore(
                    QUARTER_HOUR_RESOLUTION_SECONDS,
                    retentionCutoff(now, config.quarterHourRetentionDays())
            );
            deleteBefore(HOURLY_RESOLUTION_SECONDS, retentionCutoff(now, config.hourlyRetentionDays()));
        });
    }

    List<MonitorAggregate> readRange(int resolutionSeconds, long from, long to) throws SQLException {
        ensureOpen();
        long resolutionMillis = resolutionSeconds * 1000L;
        long lowerBound = from > Long.MIN_VALUE + resolutionMillis
                ? from - resolutionMillis + 1
                : Long.MIN_VALUE;

        String sql = "SELECT " + AGGREGATE_COLUMNS + " FROM monitor_aggregate "
                + "WHERE resolution_seconds = ? AND bucket_start >= ? AND bucket_start < ? "
                + "ORDER BY bucket_start";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resolutionSeconds);
            statement.setLong(2, lowerBound);
            statement.setLong(3, to);
            try(ResultSet result = statement.executeQuery()) {
                List<MonitorAggregate> aggregates = new ArrayList<>();
                while(result.next()) {
                    aggregates.add(readAggregate(result));
                }
                return aggregates;
            }
        }
    }

    private void initializeSchema() throws SQLException {
        ensureOpen();
        connection.setAutoCommit(false);
        try(Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS monitor_schema_version (
                        version INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS monitor_aggregate (
                        resolution_seconds INTEGER NOT NULL,
                        bucket_start BIGINT NOT NULL,
                        sample_count BIGINT NOT NULL,
                        cpu_sum DOUBLE PRECISION NOT NULL,
                        cpu_min DOUBLE PRECISION NOT NULL,
                        cpu_max DOUBLE PRECISION NOT NULL,
                        memory_sum DOUBLE PRECISION NOT NULL,
                        memory_min DOUBLE PRECISION NOT NULL,
                        memory_max DOUBLE PRECISION NOT NULL,
                        jvm_memory_sum DOUBLE PRECISION NOT NULL,
                        jvm_memory_min DOUBLE PRECISION NOT NULL,
                        jvm_memory_max DOUBLE PRECISION NOT NULL,
                        tps_sum DOUBLE PRECISION NOT NULL,
                        tps_min DOUBLE PRECISION NOT NULL,
                        tps_max DOUBLE PRECISION NOT NULL,
                        network_upload_sum DOUBLE PRECISION NOT NULL,
                        network_upload_min DOUBLE PRECISION NOT NULL,
                        network_upload_max DOUBLE PRECISION NOT NULL,
                        network_download_sum DOUBLE PRECISION NOT NULL,
                        network_download_min DOUBLE PRECISION NOT NULL,
                        network_download_max DOUBLE PRECISION NOT NULL,
                        disk_read_sum DOUBLE PRECISION NOT NULL,
                        disk_read_min DOUBLE PRECISION NOT NULL,
                        disk_read_max DOUBLE PRECISION NOT NULL,
                        disk_write_sum DOUBLE PRECISION NOT NULL,
                        disk_write_min DOUBLE PRECISION NOT NULL,
                        disk_write_max DOUBLE PRECISION NOT NULL,
                        PRIMARY KEY (resolution_seconds, bucket_start)
                    )
                    """);

            int version = readSchemaVersion();
            if(version == 0) {
                try(PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO monitor_schema_version (version) VALUES (?)"
                )) {
                    insert.setInt(1, SCHEMA_VERSION);
                    insert.executeUpdate();
                }
            } else if(version != SCHEMA_VERSION) {
                throw new SQLException("Unsupported monitor history schema version: " + version);
            }
            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private int readSchemaVersion() throws SQLException {
        try(Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT version FROM monitor_schema_version")) {
            if(!result.next()) return 0;
            int version = result.getInt(1);
            if(result.next()) throw new SQLException("Monitor history schema version table contains multiple rows.");
            return version;
        }
    }

    private void rollUpAfterMinute(long minuteStart, long completedBefore) throws SQLException {
        long quarterHourStart = MonitorHistoryAccumulator.align(
                minuteStart,
                QUARTER_HOUR_RESOLUTION_SECONDS * 1000L
        );
        if(bucketEnd(quarterHourStart, QUARTER_HOUR_RESOLUTION_SECONDS) > completedBefore) return;

        rebuildBucket(MINUTE_RESOLUTION_SECONDS, QUARTER_HOUR_RESOLUTION_SECONDS, quarterHourStart);

        long hourStart = MonitorHistoryAccumulator.align(
                quarterHourStart,
                HOURLY_RESOLUTION_SECONDS * 1000L
        );
        if(bucketEnd(hourStart, HOURLY_RESOLUTION_SECONDS) <= completedBefore) {
            rebuildBucket(QUARTER_HOUR_RESOLUTION_SECONDS, HOURLY_RESOLUTION_SECONDS, hourStart);
        }
    }

    private void rebuildCompleted(int sourceResolution, int targetResolution, long now) throws SQLException {
        long targetResolutionMillis = targetResolution * 1000L;
        long completedBefore = MonitorHistoryAccumulator.align(now, targetResolutionMillis);
        List<MonitorAggregate> source = readRange(sourceResolution, Long.MIN_VALUE, completedBefore);
        Map<Long, List<MonitorAggregate>> grouped = new LinkedHashMap<>();
        for(MonitorAggregate aggregate : source) {
            long targetStart = MonitorHistoryAccumulator.align(aggregate.bucketStart(), targetResolutionMillis);
            grouped.computeIfAbsent(targetStart, ignored -> new ArrayList<>()).add(aggregate);
        }
        for(Map.Entry<Long, List<MonitorAggregate>> entry : grouped.entrySet()) {
            MonitorAggregate aggregate = MonitorAggregate.combine(targetResolution, entry.getKey(), entry.getValue());
            if(aggregate != null) upsertAggregate(aggregate);
        }
    }

    private void rebuildBucket(int sourceResolution, int targetResolution, long targetStart) throws SQLException {
        long targetEnd = bucketEnd(targetStart, targetResolution);
        List<MonitorAggregate> source = readRange(sourceResolution, targetStart, targetEnd);
        MonitorAggregate aggregate = MonitorAggregate.combine(targetResolution, targetStart, source);
        if(aggregate != null) upsertAggregate(aggregate);
    }

    private MonitorAggregate readAggregate(int resolutionSeconds, long bucketStart) throws SQLException {
        String sql = "SELECT " + AGGREGATE_COLUMNS + " FROM monitor_aggregate "
                + "WHERE resolution_seconds = ? AND bucket_start = ?";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resolutionSeconds);
            statement.setLong(2, bucketStart);
            try(ResultSet result = statement.executeQuery()) {
                return result.next() ? readAggregate(result) : null;
            }
        }
    }

    private MonitorAggregate readAggregate(ResultSet result) throws SQLException {
        int column = 1;
        int resolutionSeconds = result.getInt(column++);
        long bucketStart = result.getLong(column++);
        long sampleCount = result.getLong(column++);

        double[] sums = new double[8];
        double[] minima = new double[8];
        double[] maxima = new double[8];
        for(int i = 0; i < sums.length; i++) {
            sums[i] = result.getDouble(column++);
            minima[i] = result.getDouble(column++);
            maxima[i] = result.getDouble(column++);
        }

        return new MonitorAggregate(
                resolutionSeconds,
                bucketStart,
                sampleCount,
                toMonitorData(sums),
                toMonitorData(minima),
                toMonitorData(maxima)
        );
    }

    private void upsertAggregate(MonitorAggregate aggregate) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            int parameter = 1;
            statement.setInt(parameter++, aggregate.resolutionSeconds());
            statement.setLong(parameter++, aggregate.bucketStart());
            statement.setLong(parameter++, aggregate.sampleCount());

            parameter = setMetric(statement, parameter, aggregate.sum().cpu(), aggregate.minimum().cpu(), aggregate.maximum().cpu());
            parameter = setMetric(statement, parameter, aggregate.sum().memory(), aggregate.minimum().memory(), aggregate.maximum().memory());
            parameter = setMetric(statement, parameter, aggregate.sum().jvmMemory(), aggregate.minimum().jvmMemory(), aggregate.maximum().jvmMemory());
            parameter = setMetric(statement, parameter, aggregate.sum().tps(), aggregate.minimum().tps(), aggregate.maximum().tps());
            parameter = setMetric(statement, parameter, aggregate.sum().networkUpload(), aggregate.minimum().networkUpload(), aggregate.maximum().networkUpload());
            parameter = setMetric(statement, parameter, aggregate.sum().networkDownload(), aggregate.minimum().networkDownload(), aggregate.maximum().networkDownload());
            parameter = setMetric(statement, parameter, aggregate.sum().diskRead(), aggregate.minimum().diskRead(), aggregate.maximum().diskRead());
            setMetric(statement, parameter, aggregate.sum().diskWrite(), aggregate.minimum().diskWrite(), aggregate.maximum().diskWrite());

            statement.executeUpdate();
        }
    }

    private int setMetric(PreparedStatement statement, int parameter, double sum, double minimum, double maximum)
            throws SQLException {
        statement.setDouble(parameter++, sum);
        statement.setDouble(parameter++, minimum);
        statement.setDouble(parameter++, maximum);
        return parameter;
    }

    private void deleteBefore(int resolutionSeconds, long cutoff) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM monitor_aggregate WHERE resolution_seconds = ? AND bucket_start < ?"
        )) {
            statement.setInt(1, resolutionSeconds);
            statement.setLong(2, cutoff);
            statement.executeUpdate();
        }
    }

    private long retentionCutoff(long now, int retentionDays) {
        return now - retentionDays * DAY_MILLIS;
    }

    private long bucketEnd(long bucketStart, int resolutionSeconds) {
        return bucketStart + resolutionSeconds * 1000L;
    }

    private MonitorData toMonitorData(double[] values) {
        return new MonitorData(
                values[0],
                values[1],
                values[2],
                values[3],
                values[4],
                values[5],
                values[6],
                values[7]
        );
    }

    private void transaction(SqlRunnable operation) throws SQLException {
        ensureOpen();
        connection.setAutoCommit(false);
        try {
            operation.run();
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void rollback() {
        if(connection == null) return;
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The original database exception is more useful to callers.
        }
    }

    private void ensureOpen() throws SQLException {
        if(!isOpen()) throw new SQLException("Monitor history database is not open.");
    }

    @Override
    public void close() throws SQLException {
        if(connection == null) return;
        try {
            connection.close();
        } finally {
            connection = null;
        }
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
