package net.opanel.monitor;

import net.opanel.common.OPanelServer;
import java.util.*;

/** One finite capture per incident. No loaded-chunk enumeration or forced loads. */
public final class JavaIncidentSceneSampler {
    private record Key(String world, int x, int z) {}
    private static String seen;
    private static String capturing;
    private static long requested;
    private static int candidates;
    private static final ArrayDeque<Key> pending = new ArrayDeque<>();
    private static final List<IncidentScene.Chunk> chunks = new ArrayList<>();
    private static final List<IncidentScene.Player> players = new ArrayList<>();
    private JavaIncidentSceneSampler() {}
    public static void tick(OPanelServer server, double executionMs) {
        if(!PerformanceTickSampler.enabled()) { capturing = seen = null; pending.clear(); chunks.clear(); players.clear(); return; }
        String active = PerformanceTickSampler.activeId();
        long now = System.currentTimeMillis();
        if(active != null && !active.equals(seen)) {
            if(capturing != null) finish(now);
            seen = capturing = active;
            requested = now;
            chunks.clear(); players.clear(); pending.clear();
            Set<Key> keys = new LinkedHashSet<>();
            var snapshot = server.getMapPlayers();
            for(var player : snapshot == null ? java.util.List.<net.opanel.map.MapPlayerPositions.Position>of() : snapshot) {
                if(players.size() >= 64) break;
                String world = player.world();
                players.add(new IncidentScene.Player(player.uuid(), player.name(), world, player.x(), player.y(), player.z()));
                int x = ((int)Math.floor(player.x())) >> 4, z = ((int)Math.floor(player.z())) >> 4;
                for(int dx = -1; dx <= 1; dx++) for(int dz = -1; dz <= 1; dz++)
                    if(keys.size() < 64) keys.add(new Key(world, x + dx, z + dz));
            }
            pending.addAll(keys); candidates = keys.size();
            publish(0);
        }
        if(capturing == null) return;
        if(now - requested >= 30000 || pending.isEmpty()) { finish(now); return; }
        if(!Double.isFinite(executionMs) || executionMs > 40) return;
        Key key = pending.removeFirst();
        var accessor = server.getChunkAccessor();
        IncidentScene.Chunk chunk;
        try { chunk = accessor == null ? null : accessor.readIncidentChunk(key.world(), key.x(), key.z(), now); }
        catch(RuntimeException e) { chunk = null; }
        chunks.add(chunk == null ? new IncidentScene.Chunk(key.world(), key.x(), key.z(), now, "entities-unavailable", Map.of(), Map.of()) : chunk);
        if(pending.isEmpty()) finish(now);
    }
    private static void publish(long completed) {
        PerformanceTickSampler.attachScene(capturing, new IncidentScene(requested, completed,
            "player-neighborhoods-limited-not-cause-attribution", candidates, chunks, players));
    }
    private static void finish(long now) { publish(now); capturing = null; pending.clear(); chunks.clear(); players.clear(); }
}
