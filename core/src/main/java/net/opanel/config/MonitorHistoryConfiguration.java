package net.opanel.config;

import net.opanel.logger.Loggable;

public record MonitorHistoryConfiguration(
        boolean enabled,
        int minuteRetentionDays,
        int quarterHourRetentionDays,
        int hourlyRetentionDays
) {
    private static final int MIN_MINUTE_RETENTION_DAYS = 1;
    private static final int MAX_MINUTE_RETENTION_DAYS = 30;
    private static final int MIN_QUARTER_HOUR_RETENTION_DAYS = 1;
    private static final int MAX_QUARTER_HOUR_RETENTION_DAYS = 365;
    private static final int MIN_HOURLY_RETENTION_DAYS = 1;
    private static final int MAX_HOURLY_RETENTION_DAYS = 3650;

    public static MonitorHistoryConfiguration from(OPanelConfiguration source, Loggable logger) {
        int minute = clamp(
                source.monitorHistoryMinuteRetentionDays,
                MIN_MINUTE_RETENTION_DAYS,
                MAX_MINUTE_RETENTION_DAYS
        );
        int quarterHour = Math.max(minute, clamp(
                source.monitorHistoryQuarterHourRetentionDays,
                MIN_QUARTER_HOUR_RETENTION_DAYS,
                MAX_QUARTER_HOUR_RETENTION_DAYS
        ));
        int hourly = Math.max(quarterHour, clamp(
                source.monitorHistoryHourlyRetentionDays,
                MIN_HOURLY_RETENTION_DAYS,
                MAX_HOURLY_RETENTION_DAYS
        ));

        if(minute != source.monitorHistoryMinuteRetentionDays
                || quarterHour != source.monitorHistoryQuarterHourRetentionDays
                || hourly != source.monitorHistoryHourlyRetentionDays) {
            logger.warn(
                    "Invalid monitor history retention configuration; using effective values "
                            + minute + "/" + quarterHour + "/" + hourly + " days."
            );
        }

        return new MonitorHistoryConfiguration(
                source.monitorHistoryEnabled,
                minute,
                quarterHour,
                hourly
        );
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
