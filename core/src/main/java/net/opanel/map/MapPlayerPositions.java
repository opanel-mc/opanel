package net.opanel.map;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Immutable positions published from the owning server/entity threads. */
public final class MapPlayerPositions {
    public static final long SAMPLE_INTERVAL_MS = 5000;
    public static final long VIEWER_LEASE_MS = 7500;
    public static final long STALE_AFTER_MS = 15000;
    public record Position(String uuid, String name, String world, double x, double y, double z) {}
    public record Snapshot(long sampledAt, List<Position> players) {
        public Snapshot { players = List.copyOf(players); }
    }
    private static volatile Snapshot snapshot = new Snapshot(0, List.of());
    private static final AtomicLong demandedUntil = new AtomicLong();
    private MapPlayerPositions() {}
    public static Snapshot snapshot() { return snapshot; }
    public static void request(long now) { demandedUntil.set(now + VIEWER_LEASE_MS); }
    public static boolean hasDemand() { return demandedUntil.get() != 0; }
    public static boolean needsSample(long now) {
        long deadline = demandedUntil.get();
        if(deadline == 0) return false;
        if(now > deadline) { demandedUntil.compareAndSet(deadline, 0); return false; }
        return snapshot.sampledAt() == 0 || now < snapshot.sampledAt() || now - snapshot.sampledAt() >= SAMPLE_INTERVAL_MS;
    }
    public static void publish(long now, List<Position> players) { snapshot = new Snapshot(now, players); }
    public static void reset() { snapshot = new Snapshot(0, List.of()); demandedUntil.set(0); }
}
