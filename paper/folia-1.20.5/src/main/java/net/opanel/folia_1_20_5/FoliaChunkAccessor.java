package net.opanel.folia_1_20_5;

import net.opanel.annotation.Rewrite;
import net.opanel.paper_helper.BasePaperChunkAccessor;
import net.opanel.map.Tile;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FoliaChunkAccessor extends BasePaperChunkAccessor {
    public FoliaChunkAccessor(Main plugin) {
        super(plugin);
    }

    @Rewrite
    @Override
    public Tile readLiveTile(int chunkX, int chunkZ) {
        return readLiveTile(plugin.getServer().getWorlds().get(0).getName(), chunkX, chunkZ);
    }

    @Override
    public Tile readLiveTile(String worldName, int chunkX, int chunkZ) {
        Server server = plugin.getServer();

        try {
            World world = server.getWorld(worldName);
            if(world == null) return null;

            CompletableFuture<Tile> future = new CompletableFuture<>();
            server.getRegionScheduler().run(plugin, world, chunkX, chunkZ, task -> {
                try {
                    future.complete(readWorld(world, chunkX, chunkZ));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future.get(SYNC_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }
}
