package net.opanel.fabric_helper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.chunk.WorldChunk;
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
    protected net.minecraft.server.world.ServerWorld getMapWorld() {
        if(requestedWorld != null && requestedWorld.endsWith("_nether")) return server.getWorld(net.minecraft.world.World.NETHER);
        if(requestedWorld != null && requestedWorld.endsWith("_the_end")) return server.getWorld(net.minecraft.world.World.END);
        return server.getOverworld();
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
    abstract protected Tile.Section buildSection(WorldChunk chunk, int sectionY);
}
