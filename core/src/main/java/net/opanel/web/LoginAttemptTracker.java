package net.opanel.web;

import java.util.HashMap;
import java.util.Map;

public final class LoginAttemptTracker {
    private static final int MAX_FAILURES = 5;
    private static final int MAX_TRACKED_IPS = 10_000;
    private static final long FAILURE_WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final long BAN_PERIOD_MILLIS = 10 * 60 * 1000L;
    private static final long CLEANUP_INTERVAL_MILLIS = 60 * 1000L;
    private static final long CAPACITY_RETRY_AFTER_SECONDS = 60;

    private final Map<String, AttemptRecord> records = new HashMap<>();
    private long nextCleanupAt = System.currentTimeMillis() + CLEANUP_INTERVAL_MILLIS;

    public synchronized Result check(String ip) {
        long now = System.currentTimeMillis();
        cleanupExpiredIfDue(now);

        AttemptRecord record = getActiveRecord(ip, now);
        if(record == null) {
            if(records.size() >= MAX_TRACKED_IPS) {
                return Result.capacityFull();
            }
            return Result.allowed(0);
        }

        if(record.bannedUntil > now) {
            return Result.banned(secondsUntil(record.bannedUntil, now));
        }

        return Result.allowed(record.failedAttempts);
    }

    public synchronized Result recordFailure(String ip) {
        long now = System.currentTimeMillis();
        cleanupExpiredIfDue(now);

        AttemptRecord record = getActiveRecord(ip, now);
        if(record == null) {
            if(records.size() >= MAX_TRACKED_IPS) {
                return Result.capacityFull();
            }
            record = new AttemptRecord(0, now, 0);
        }

        if(record.bannedUntil > now) {
            return Result.banned(secondsUntil(record.bannedUntil, now));
        }

        int failedAttempts = record.failedAttempts + 1;
        long windowStartedAt = record.failedAttempts == 0 ? now : record.windowStartedAt;
        long bannedUntil = failedAttempts >= MAX_FAILURES ? now + BAN_PERIOD_MILLIS : 0;
        records.put(ip, new AttemptRecord(failedAttempts, windowStartedAt, bannedUntil));
        return Result.allowed(failedAttempts);
    }

    public synchronized void recordSuccess(String ip) {
        records.remove(ip);
    }

    private AttemptRecord getActiveRecord(String ip, long now) {
        AttemptRecord record = records.get(ip);
        if(record != null && isExpired(record, now)) {
            records.remove(ip);
            return null;
        }
        return record;
    }

    private void cleanupExpiredIfDue(long now) {
        if(now < nextCleanupAt) return;

        records.values().removeIf(record -> isExpired(record, now));
        nextCleanupAt = now + CLEANUP_INTERVAL_MILLIS;
    }

    private boolean isExpired(AttemptRecord record, long now) {
        return record.bannedUntil > 0
                ? now >= record.bannedUntil
                : now - record.windowStartedAt >= FAILURE_WINDOW_MILLIS;
    }

    private long secondsUntil(long target, long now) {
        return Math.max(1, (target - now + 999) / 1000);
    }

    private record AttemptRecord(
            int failedAttempts,
            long windowStartedAt,
            long bannedUntil
    ) {}

    public enum Status {
        ALLOWED,
        BANNED,
        CAPACITY_FULL
    }

    public record Result(Status status, int failedAttempts, long retryAfterSeconds) {
        private static Result allowed(int failedAttempts) {
            return new Result(Status.ALLOWED, failedAttempts, 0);
        }

        private static Result banned(long retryAfterSeconds) {
            return new Result(Status.BANNED, 0, retryAfterSeconds);
        }

        private static Result capacityFull() {
            return new Result(Status.CAPACITY_FULL, 0, CAPACITY_RETRY_AFTER_SECONDS);
        }
    }
}
