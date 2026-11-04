package net.opanel.map;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
public class MapPlayerPositionsTest {
    @Test public void snapshotsAreImmutableAndReplaceLeavingPlayers() {
        MapPlayerPositions.reset();
        var values = new ArrayList<MapPlayerPositions.Position>();
        values.add(new MapPlayerPositions.Position("id", "Alex", "world", 1, 64, -2));
        MapPlayerPositions.publish(1000, values);
        MapPlayerPositions.request(1000);
        values.clear();
        var snapshot = MapPlayerPositions.snapshot();
        assertEquals(1, snapshot.players().size());
        assertFalse(MapPlayerPositions.needsSample(1500));
        assertFalse(MapPlayerPositions.needsSample(5999));
        assertTrue(MapPlayerPositions.needsSample(6000));
        MapPlayerPositions.publish(6000, List.of());
        MapPlayerPositions.request(6000);
        assertTrue(MapPlayerPositions.snapshot().players().isEmpty());
        assertEquals(1, snapshot.players().size());
    }
    @Test public void clockRollbackAndResetDoNotRetainStaleLocations() {
        MapPlayerPositions.reset();
        MapPlayerPositions.publish(2000, List.of());
        MapPlayerPositions.request(1000);
        assertTrue(MapPlayerPositions.needsSample(1000));
        MapPlayerPositions.reset();
        assertEquals(0, MapPlayerPositions.snapshot().sampledAt());
    }
    @Test public void idleAndExpiredViewersDoNotRequestSampling() {
        MapPlayerPositions.reset();
        assertFalse(MapPlayerPositions.hasDemand());
        assertFalse(MapPlayerPositions.needsSample(10000));
        MapPlayerPositions.request(10000);
        assertTrue(MapPlayerPositions.needsSample(10000));
        assertFalse(MapPlayerPositions.needsSample(17501));
        assertFalse(MapPlayerPositions.hasDemand());
    }
}
