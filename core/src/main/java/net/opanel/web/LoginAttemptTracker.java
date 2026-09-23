package net.opanel.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LoginAttemptTracker {
    private static final int MAX_FAILURES = 5;
    private static final int MAX_TRACKED_IPS = 10_000;
    private static final long FAILURE_WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final long BAN_PERIOD_MILLIS = 10 * 60 * 1000L;
    private static final long CAPACITY_RETRY_AFTER_SECONDS = 60;

    private final Map<String, AttemptRecord> records = new HashMap<>();

    public synchronized Result check(String ip) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);

        AttemptRecord record = records.get(ip);
        if(record == null) {
            if(records.size() >= MAX_TRACKED_IPS) {
                return Result.capacityFull();
            }
            records.put(ip, new AttemptRecord(0, now, 0, now));
            return Result.allowed(0);
        }

        if(record.bannedUntil > now) {
            return Result.banned(secondsUntil(record.bannedUntil, now));
        }

        records.put(ip, new AttemptRecord(
                record.failedAttempts,
                record.windowStartedAt,
                0,
                now
        ));
        return Result.allowed(record.failedAttempts);
    }

    public synchronized Result recordFailure(String ip) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);

        AttemptRecord record = records.get(ip);
        if(record == null) {
            if(records.size() >= MAX_TRACKED_IPS) {
                return Result.capacityFull();
            }
            record = new AttemptRecord(0, now, 0, now);
        }

        if(record.bannedUntil > now) {
            return Result.banned(secondsUntil(record.bannedUntil, now));
        }

        int failedAttempts = record.failedAttempts + 1;
        long windowStartedAt = record.failedAttempts == 0 ? now : record.windowStartedAt;
        long bannedUntil = failedAttempts >= MAX_FAILURES ? now + BAN_PERIOD_MILLIS : 0;
        records.put(ip, new AttemptRecord(failedAttempts, windowStartedAt, bannedUntil, now));
        return Result.allowed(failedAttempts);
    }

    public synchronized void recordSuccess(String ip) {
        records.remove(ip);
    }

    private void cleanupExpired(long now) {
        Iterator<AttemptRecord> iterator = records.values().iterator();
        while(iterator.hasNext()) {
            AttemptRecord record = iterator.next();
            boolean expired = record.bannedUntil > 0
                    ? now >= record.bannedUntil
                    : record.failedAttempts == 0
                            ? now - record.lastUpdatedAt >= FAILURE_WINDOW_MILLIS
                            : now - record.windowStartedAt >= FAILURE_WINDOW_MILLIS;
            if(expired) iterator.remove();
        }
    }

    private long secondsUntil(long target, long now) {
        return Math.max(1, (target - now + 999) / 1000);
    }

    private record AttemptRecord(
            int failedAttempts,
            long windowStartedAt,
            long bannedUntil,
            long lastUpdatedAt
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
