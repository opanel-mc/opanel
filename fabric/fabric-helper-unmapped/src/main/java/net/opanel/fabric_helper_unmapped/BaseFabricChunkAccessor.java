package net.opanel.fabric_helper_unmapped;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.opanel.common.OPanelChunkAccessor;
import net.opanel.map.Tile;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class BaseFabricChunkAccessor implements OPanelChunkAccessor {
    protected final MinecraftServer server;

    public BaseFabricChunkAccessor(MinecraftServer server) {
        this.server = server;
    }

    private String requestedWorld;
    protected net.minecraft.server.level.ServerLevel getMapWorld() {
        if(requestedWorld != null && requestedWorld.endsWith("_nether")) return server.getLevel(net.minecraft.world.level.Level.NETHER);
        if(requestedWorld != null && requestedWorld.endsWith("_the_end")) return server.getLevel(net.minecraft.world.level.Level.END);
        return server.overworld();
    }
    protected abstract int sceneMinY();
    protected abstract int sceneMaxY();
    @Override
    public net.opanel.monitor.IncidentScene.Chunk readIncidentChunk(String name, int x, int z, long now) {
        requestedWorld = name;
        try {
            var world = getMapWorld();
            var chunk = world == null ? null : world.getChunkSource().getChunkNow(x, z);
            if(chunk == null) return new net.opanel.monitor.IncidentScene.Chunk(name, x, z, now, "unloaded", java.util.Map.of(), java.util.Map.of());
            var entities = new java.util.HashMap<String, Integer>();
            var blocks = new java.util.HashMap<String, Integer>();
            boolean truncated = false;
            int count = 0;
            for(var entity : world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(x * 16.0, sceneMinY(), z * 16.0, x * 16.0 + 16, sceneMaxY(), z * 16.0 + 16))) {
                if(Math.floor(entity.getX() / 16.0) != x || Math.floor(entity.getZ() / 16.0) != z) continue;
                if(count++ >= 1024) { truncated = true; break; }
                String type = entity.getType().getDescriptionId().replace("entity.minecraft.", "").toUpperCase(java.util.Locale.ROOT);
                entities.merge(type, 1, Integer::sum);
            }
            count = 0;
            for(var block : chunk.getBlockEntities().values()) {
                if(count++ >= 1024) { truncated = true; break; }
                String type = block.getBlockState().getBlock().getDescriptionId().replace("block.minecraft.", "").toUpperCase(java.util.Locale.ROOT);
                blocks.merge(type, 1, Integer::sum);
            }
            return new net.opanel.monitor.IncidentScene.Chunk(name, x, z, now, truncated ? "truncated" : "sampled", entities, blocks);
        } finally { requestedWorld = null; }
    }

    @Override
    public Tile readLiveTile(String worldName, int chunkX, int chunkZ) {
        try {
            return server.submit(() -> {
                requestedWorld = worldName;
                try { return readOnMainThread(chunkX, chunkZ); }
                finally { requestedWorld = null; }
            }).get(SYNC_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch(Exception e) { return null; }
    }

    @Override
    public Tile readLiveTile(int chunkX, int chunkZ) {
        try {
            Future<Tile> future = server.submit(() -> readOnMainThread(chunkX, chunkZ));
            return future.get(SYNC_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    abstract protected Tile readOnMainThread(int chunkX, int chunkZ);
    abstract protected Tile.Section buildSection(LevelChunk chunk, int sectionY);
}
