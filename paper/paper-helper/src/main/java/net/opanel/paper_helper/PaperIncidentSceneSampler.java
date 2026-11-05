package net.opanel.paper_helper;

import net.opanel.monitor.IncidentScene;
import net.opanel.monitor.PerformanceTickSampler;
import org.bukkit.Bukkit;
import java.util.*;

/** One finite capture per incident. No loaded-chunk enumeration or forced loads. */
public final class PaperIncidentSceneSampler {
    private static final class CollectionLimitReached extends RuntimeException {
        @Override public synchronized Throwable fillInStackTrace() { return this; }
    }
    private record Key(String world, int x, int z) {}
    private static String seen;
    private static String capturing;
    private static long requested;
    private static int candidates;
    private static final ArrayDeque<Key> pending = new ArrayDeque<>();
    private static final List<IncidentScene.Chunk> chunks = new ArrayList<>();
    private static final List<IncidentScene.Player> players = new ArrayList<>();
    private static final java.lang.reflect.Method ENTITIES_LOADED = findEntitiesLoaded();
    private static final java.lang.reflect.Method FILTERED_BLOCK_ENTITIES = findFilteredBlockEntities();
    private static java.lang.reflect.Method findFilteredBlockEntities() {
        try { return org.bukkit.Chunk.class.getMethod("getTileEntities", java.util.function.Predicate.class, boolean.class); }
        catch(NoSuchMethodException e) { return null; }
    }
    private static java.lang.reflect.Method findEntitiesLoaded() {
        try { return org.bukkit.Chunk.class.getMethod("isEntitiesLoaded"); }
        catch(NoSuchMethodException e) { return null; }
    }
    private static boolean entitiesLoaded(org.bukkit.Chunk chunk) {
        if(ENTITIES_LOADED == null) return false;
        try { return Boolean.TRUE.equals(ENTITIES_LOADED.invoke(chunk)); }
        catch(ReflectiveOperationException e) { return false; }
    }
    private PaperIncidentSceneSampler() {}
    public static void tick(double executionMs) {
        if(!PerformanceTickSampler.enabled()) { capturing = seen = null; pending.clear(); chunks.clear(); players.clear(); return; }
        String active = PerformanceTickSampler.activeId();
        long now = System.currentTimeMillis();
        if(active != null && !active.equals(seen)) {
            if(capturing != null) finish(now);
            seen = capturing = active;
            requested = now;
            chunks.clear(); players.clear(); pending.clear();
            Set<Key> keys = new LinkedHashSet<>();
            for(var player : Bukkit.getOnlinePlayers()) {
                if(players.size() >= 64) break;
                var pos = player.getLocation();
                String world = player.getWorld().getName();
                players.add(new IncidentScene.Player(player.getUniqueId().toString(), player.getName(), world, pos.getX(), pos.getY(), pos.getZ()));
                int x = pos.getBlockX() >> 4, z = pos.getBlockZ() >> 4;
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
        var world = Bukkit.getWorld(key.world());
        String status = "unloaded";
        Map<String,Integer> entities = new HashMap<>(), blocks = new HashMap<>();
        if(world != null && world.isChunkLoaded(key.x(), key.z())) {
            var chunk = world.getChunkAt(key.x(), key.z());
            if(entitiesLoaded(chunk)) {
                status = "sampled";
                // Bukkit's entity queries allocate the whole result before applying
                // predicates. Only query when the world-wide count bounds that allocation.
                if(world.getEntityCount() <= 1024) {
                    for(var entity : chunk.getEntities()) entities.merge(entity.getType().name(), 1, Integer::sum);
                } else {
                    status = "entities-unavailable";
                }
                final int[] blockCount = {0};
                java.util.function.Predicate<org.bukkit.block.Block> blockFilter = block -> {
                        if(blockCount[0]++ >= 1024) throw new CollectionLimitReached();
                        blocks.merge(block.getType().name(), 1, Integer::sum);
                        return false;
                    };
                if(FILTERED_BLOCK_ENTITIES == null) status = "entities-unavailable";
                else try {
                    FILTERED_BLOCK_ENTITIES.invoke(chunk, blockFilter, false);
                } catch(java.lang.reflect.InvocationTargetException e) {
                    if(e.getCause() instanceof CollectionLimitReached) {
                        if(!status.equals("entities-unavailable")) status = "truncated";
                    } else status = "entities-unavailable";
                } catch(ReflectiveOperationException e) {
                    status = "entities-unavailable";
                }
            } else status = "entities-unavailable";
        }
        chunks.add(new IncidentScene.Chunk(key.world(), key.x(), key.z(), now, status, entities, blocks));
        if(pending.isEmpty()) finish(now);
    }
    private static void publish(long completed) {
        PerformanceTickSampler.attachScene(capturing, new IncidentScene(requested, completed,
            "player-neighborhoods-limited-not-cause-attribution", candidates, chunks, players));
    }
    private static void finish(long now) { publish(now); capturing = null; pending.clear(); chunks.clear(); players.clear(); }
}
