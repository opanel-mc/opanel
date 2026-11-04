package net.opanel.monitor;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.google.gson.Gson;
public class IncidentSceneTest {
    @Test public void evidenceSurvivesRecoveryJsonAndRestart() {
        long now = System.currentTimeMillis();
        var recorder = new PerformanceIncidentRecorder(50, 1, 1);
        recorder.record(now, 80);
        String id = recorder.activeId();
        var chunks = new ArrayList<IncidentScene.Chunk>();
        chunks.add(new IncidentScene.Chunk("world_nether", -2, 3, now, "sampled", Map.of("ITEM", 12), Map.of("HOPPER", 2)));
        var scene = new IncidentScene(now, now + 10, "limited", 1, chunks, List.of(new IncidentScene.Player("id", "Alex", "world_nether", -20, 64, 50)));
        recorder.attachScene(id, scene);
        chunks.clear();
        recorder.record(now + 1000, 10);
        Gson gson = new Gson();
        var saved = gson.fromJson(gson.toJson(recorder.snapshot().get(0)), PerformanceIncidentRecorder.Incident.class);
        var restored = new PerformanceIncidentRecorder(50, 1, 1);
        restored.restore(List.of(saved));
        assertEquals(12, restored.snapshot().get(0).scene().chunks().get(0).entities().get("ITEM").intValue());
        assertEquals("Alex", restored.snapshot().get(0).scene().players().get(0).name());
        assertEquals("recovered", restored.snapshot().get(0).status());
    }
    @Test public void oldRecordsWithoutSceneRemainReadable() {
        var recorder = new PerformanceIncidentRecorder(50, 1, 1);
        long now = System.currentTimeMillis();
        recorder.restore(List.of(new PerformanceIncidentRecorder.Incident("old", now, now, "recovered", 50, List.of())));
        assertNull(recorder.snapshot().get(0).scene());
    }
}
