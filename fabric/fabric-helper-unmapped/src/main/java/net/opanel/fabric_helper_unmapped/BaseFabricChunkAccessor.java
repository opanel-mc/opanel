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
