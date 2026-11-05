package net.opanel.monitor;

import java.nio.file.Files;

public class PerformanceIncidentStoreTest {
    @org.junit.Test
    public void verifiesBehavior() throws Exception { main(new String[0]); }

    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("opanel-incidents-");
        var path = directory.resolve("incidents.json");
        try {
            var recorder = new PerformanceIncidentRecorder(50, 2, 2);
            long now = System.currentTimeMillis();
            recorder.record(now - 1000, 80);
            recorder.record(now, 90);
            String id = recorder.snapshot().get(0).id();
            PerformanceTickSampler.reset();
            PerformanceTickSampler.restore(recorder.snapshot());
            var store = new PerformanceIncidentStore(path);
            store.save(true);
            PerformanceTickSampler.reset();
            store.load();
            var restored = PerformanceTickSampler.incidents();
            if(restored.size() != 1 || !restored.get(0).id().equals(id)
                    || !restored.get(0).status().equals("interrupted") || restored.get(0).samples().size() != 2) {
                throw new AssertionError("Restart must preserve evidence and mark interrupted events");
            }
            String largeId = "x".repeat(9 * 1024 * 1024);
            var newest = new PerformanceIncidentRecorder.Incident(largeId, now, now, "recovered", 50, java.util.List.of());
            var oldest = new PerformanceIncidentRecorder.Incident(largeId + "old", now, now, "recovered", 50, java.util.List.of());
            PerformanceTickSampler.reset();
            PerformanceTickSampler.restore(java.util.List.of(newest, oldest));
            store.save(true);
            if(Files.size(path) > 16 * 1024 * 1024) throw new AssertionError("saved evidence exceeds read limit");
            PerformanceTickSampler.reset();
            store.load();
            if(PerformanceTickSampler.incidents().size() != 1 || !PerformanceTickSampler.incidents().get(0).id().equals(largeId))
                throw new AssertionError("size limit should retain the newest readable incident");
            Files.writeString(path, "invalid-json");
            try { store.load(); throw new AssertionError("corrupt file accepted"); }
            catch(java.io.IOException expected) { }
            try { store.save(true); throw new AssertionError("corrupt evidence overwritten"); }
            catch(java.io.IOException expected) { }
            if(!Files.readString(path).equals("invalid-json")) throw new AssertionError("failed load must preserve existing file");
            System.out.println("PerformanceIncidentStore tests passed");
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }
}
