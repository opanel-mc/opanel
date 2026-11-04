package net.opanel.monitor;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.lang.reflect.Proxy;
import net.opanel.common.*;
import net.opanel.map.*;
public class JavaIncidentSceneSamplerTest {
    @Test public void capturesWithoutMapViewersAndSkipsBusyTicks() throws Exception {
        PerformanceTickSampler.configure(true, 50, 1, 1);
        var field = PerformanceTickSampler.class.getDeclaredField("recorder");
        field.setAccessible(true);
        var recorder = (PerformanceIncidentRecorder) field.get(null);
        recorder.record(System.currentTimeMillis(), 80);
        int[] reads = {0};
        OPanelChunkAccessor accessor = new OPanelChunkAccessor() {
            public Tile readLiveTile(int x, int z) { throw new AssertionError("Terrain reads are not scene sampling"); }
            public IncidentScene.Chunk readIncidentChunk(String world, int x, int z, long now) {
                reads[0]++;
                return new IncidentScene.Chunk(world, x, z, now, "sampled", Map.of("ITEM", 12), Map.of("HOPPER", 2));
            }
        };
        OPanelServer server = (OPanelServer) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{OPanelServer.class}, (object, method, args) -> {
            if(method.getName().equals("getMapPlayers")) return List.of(new MapPlayerPositions.Position("id", "Alex", "world_nether", -20, 64, 50));
            if(method.getName().equals("getChunkAccessor")) return accessor;
            throw new AssertionError(method.getName());
        });
        try {
            JavaIncidentSceneSampler.tick(server, 80);
            assertEquals(0, reads[0]);
            assertNotNull(recorder.snapshot().get(0).scene());
            for(int i = 0; i < 9; i++) JavaIncidentSceneSampler.tick(server, 10);
            var scene = recorder.snapshot().get(0).scene();
            assertEquals(9, scene.chunks().size());
            assertEquals("world_nether", scene.chunks().get(0).world());
            assertEquals("Alex", scene.players().get(0).name());
            assertTrue(scene.completedAt() > 0);
        } finally { PerformanceTickSampler.reset(); }
    }
}
