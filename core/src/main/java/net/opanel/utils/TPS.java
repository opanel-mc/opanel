package net.opanel.utils;

public class TPS {
    public static final int TICK_LIST_SIZE = 100;
    private static final long PAUSE_THRESHOLD_NS = 2000000000; // 2s
    private static long[] tickTimes = new long[TICK_LIST_SIZE];
    private static int tickIndex = 0;

    public static void onTick() {
        tickTimes[tickIndex] = System.nanoTime();
        tickIndex = (tickIndex + 1) % TICK_LIST_SIZE;
    }

    public static double getRecentMSPT() {
        long totalTime = 0;
        int ticks = 0;

        for(int i = 0; i < TICK_LIST_SIZE; i++) {
            long start = tickTimes[i];
            long end = tickTimes[(i + 1) % TICK_LIST_SIZE];

            if(start != 0 && end != 0 && end >= start) {
                totalTime += end - start;
                ticks++;
            }
        }

        return ((double) totalTime / ticks) / 1000000; // ns -> ms;
    }

    public static double getRecentTPS() {
        double tps = 1000 / getRecentMSPT();
        return Math.min(20.0, tps);
    }

    public static boolean isPaused() {
        final long current = System.nanoTime();
        int lastIndex = tickIndex - 1;
        if(lastIndex < 0) lastIndex += TICK_LIST_SIZE;
        return current - tickTimes[lastIndex] >= PAUSE_THRESHOLD_NS;
    }
}
