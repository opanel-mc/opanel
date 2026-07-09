package net.opanel.folia_1_20;

import net.opanel.annotation.Rewrite;
import net.opanel.bukkit_helper.BaseBukkitChunkAccessor;
import net.opanel.map.Tile;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FoliaChunkAccessor extends BaseBukkitChunkAccessor {
    public FoliaChunkAccessor(Main plugin) {
        super(plugin);
    }

    @Rewrite
    @Override
    public Tile readLiveTile(int chunkX, int chunkZ) {
        Server server = plugin.getServer();

        try {
            World world = server.getWorlds().get(0);
            if(world == null || world.getEnvironment() != World.Environment.NORMAL) return null;

            CompletableFuture<Tile> future = new CompletableFuture<>();
            server.getRegionScheduler().run(plugin, world, chunkX, chunkZ, task -> {
                try {
                    future.complete(readOnMainThread(chunkX, chunkZ));
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
