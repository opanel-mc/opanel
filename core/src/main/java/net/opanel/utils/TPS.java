package net.opanel.utils;

public class TPS {
    public static final int TICK_LIST_SIZE = 100;
    private static long[] tickTimes = new long[TICK_LIST_SIZE];
    private static int tickIndex = 0;

    public static void onTick() {
        tickTimes[tickIndex] = System.nanoTime();
        tickIndex = (tickIndex + 1) % TICK_LIST_SIZE;
    }

    public static double getRecentTPS() {
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

        double mspt = ((double) totalTime / ticks) / 1000000; // ns -> ms
        double tps = 1000 / mspt;

        return Math.min(20.0, tps);
    }
}
