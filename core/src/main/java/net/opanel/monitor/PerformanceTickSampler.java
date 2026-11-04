package net.opanel.monitor;

/** Platform adapters supply tick execution durations, never intervals between ticks. */
public final class PerformanceTickSampler {
    private static volatile double lastDurationMs;
    public static double lastDurationMs() { return lastDurationMs; }
    private static long tickStarted;
    public static void beginTick() {
        tickStarted = net.opanel.monitor.DiagnosticsState.enabled() && enabled ? System.nanoTime() : 0;
    }
    public static void endTick() {
        long started = tickStarted;
        tickStarted = 0;
        if(started != 0 && net.opanel.monitor.DiagnosticsState.enabled() && enabled) record((System.nanoTime() - started) / 1_000_000.0);
    }
    private static long second = -1;
    private static double sum;
    private static int count;
    private static long lastObserved;
    private static volatile boolean enabled = true;

    public static synchronized void configure(boolean value, double threshold, int trigger, int recovery) {
        PerformanceIncidentRecorder next = new PerformanceIncidentRecorder(threshold, trigger, recovery);
        reset();
        enabled = value;
        recorder = next;
    }

    public static synchronized String activeId() { return enabled ? recorder.activeId() : null; }
    public static synchronized void attachScene(String id, IncidentScene scene) { recorder.attachScene(id, scene); }
    public static boolean enabled() { return enabled; }
    private static PerformanceIncidentRecorder recorder = new PerformanceIncidentRecorder(50, 10, 5);

    private PerformanceTickSampler() {}

    public static synchronized void record(double durationMs) {
        if(!enabled || !Double.isFinite(durationMs) || durationMs < 0) return;
        lastDurationMs = durationMs;
        long now = System.currentTimeMillis();
        long bucket = now / 1000;
        if(second != bucket && count > 0) {
            recorder.record(second * 1000, sum / count);
            sum = 0;
            count = 0;
        }
        second = bucket;
        sum += durationMs;
        count++;
        lastObserved = now;
    }

    public static synchronized boolean available() {
        boolean available = enabled && lastObserved > 0 && System.currentTimeMillis() - lastObserved < 3000;
        if(!available) recorder.interrupt();
        return available;
    }

    public static synchronized java.util.List<PerformanceIncidentRecorder.Incident> incidents() {
        available();
        return recorder.snapshot();
    }

    public static synchronized void restore(java.util.List<PerformanceIncidentRecorder.Incident> records) {
        recorder.restore(records);
    }

    public static synchronized void reset() {
        tickStarted = 0;
        second = -1;
        sum = 0;
        count = 0;
        lastObserved = 0;
        enabled = true;
        recorder = new PerformanceIncidentRecorder(50, 10, 5);
    }
}
