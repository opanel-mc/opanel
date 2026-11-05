package net.opanel.monitor;

/** Run with assertions enabled; no server or third-party dependencies required. */
public class PerformanceIncidentRecorderTest {
    @org.junit.Test
    public void verifiesBehavior() throws Exception { main(new String[0]); }

    public static void main(String[] args) {
        var slow = new PerformanceIncidentRecorder(50, 10, 5);
        for(int i = 0; i < 6; i++) slow.record(1000L + i * 2000L, 2000);
        check(slow.snapshot().size() == 1, "continuous multi-second ticks must trigger an incident");
        var missing = new PerformanceIncidentRecorder(50, 3, 2);
        missing.record(1000, 100);
        missing.record(6000, 100);
        check(missing.snapshot().isEmpty(), "missing measurements must not count as a sustained breach");
        PerformanceIncidentRecorder recorder = new PerformanceIncidentRecorder(50, 3, 2);
        recorder.record(1000, 20);
        recorder.record(2000, 70);
        recorder.record(3000, 80);
        check(recorder.snapshot().isEmpty(), "must wait for sustained breach");
        recorder.record(4000, 90);
        check(recorder.snapshot().size() == 1, "one sustained incident");
        check(recorder.snapshot().get(0).startedAt() == 2000, "start at first breach");
        recorder.record(5000, 20);
        check(recorder.snapshot().get(0).endedAt() == null, "recovery needs sustained healthy data");
        recorder.record(6000, 20);
        check("recovered".equals(recorder.snapshot().get(0).status()), "recover");
        var saved = recorder.snapshot();
        recorder.record(7000, 20);
        check(saved.get(0).samples().size() < recorder.snapshot().get(0).samples().size(), "immutable snapshots");
        recorder.record(8000, 80);
        recorder.record(9000, 80);
        recorder.record(10000, 80);
        recorder.record(15000, 20);
        check("active".equals(recorder.snapshot().get(0).status()), "long tick must not close an incident");
        recorder.record(16000, 20);
        check("recovered".equals(recorder.snapshot().get(0).status()), "recovery requires observed healthy samples");
        try { recorder.record(17000, Double.NaN); throw new AssertionError("invalid sample accepted"); }
        catch(IllegalArgumentException expected) { }
        for(int i = 16; i < 20000; i++) recorder.record(i * 1000L, i % 8 < 4 ? 80 : 20);
        check(recorder.snapshot().size() <= 100, "incident count bounded");
        check(recorder.snapshot().stream().allMatch(event -> event.samples().size() <= 600), "evidence bounded");
        System.out.println("PerformanceIncidentRecorder tests passed");
    }
    private static void check(boolean condition, String message) {
        if(!condition) throw new AssertionError(message);
    }
}
