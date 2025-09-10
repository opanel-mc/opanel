package net.opanel.utils;

public class TPS {
    public static final int TICK_LIST_SIZE = 100;
    private static final long PAUSE_THRESHOLD_NS = 2_000_000_000L; // 2s
    private static final long[] tickTimes = new long[TICK_LIST_SIZE];
    private static volatile int tickIndex = 0;

    public static synchronized void onTick() {
        tickTimes[tickIndex] = System.nanoTime();
        tickIndex = (tickIndex + 1) % TICK_LIST_SIZE;
    }

    public static synchronized double getRecentMSPT() {
        long totalTime = 0;
        int validTicks = 0;

        for(int i = 0; i < TICK_LIST_SIZE - 1; i++) {
            long start = tickTimes[i];
            long end = tickTimes[(i + 1) % TICK_LIST_SIZE];

            if(start > 0 && end > 0 && end > start) {
                long duration = end - start;
                // Filter out unreasonably long durations (likely server pauses)
                if (duration < PAUSE_THRESHOLD_NS) {
                    totalTime += duration;
                    validTicks++;
                }
            }
        }

        return validTicks > 0 ? ((double) totalTime / validTicks) / 1_000_000.0 : 50.0; // ns -> ms
    }

    public static double getRecentTPS() {
        double mspt = getRecentMSPT();
        if (mspt <= 0) return 20.0;
        
        double tps = 1000.0 / mspt;
        return Math.min(20.0, Math.max(0.0, tps));
    }

    public static synchronized boolean isPaused() {
        final long current = System.nanoTime();
        int lastIndex = (tickIndex - 1 + TICK_LIST_SIZE) % TICK_LIST_SIZE;
        long lastTickTime = tickTimes[lastIndex];
        return lastTickTime > 0 && (current - lastTickTime) >= PAUSE_THRESHOLD_NS;
    }
}
