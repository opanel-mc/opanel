package net.opanel.common;

import net.opanel.map.Tile;

public interface OPanelChunkAccessor {
    int SYNC_CALL_TIMEOUT_MS = 5000;
    String FALLBACK_BIOME = "minecraft:plains";

    /**
     * Build a {@link Tile} from the live in-memory state of the chunk at
     * (chunkX, chunkZ) in the given save's overworld.
     * <p>
     * Implementations are responsible for hopping to whichever thread can
     * safely read chunk data on the underlying platform (typically the game's
     * main/server thread).
     *
     * @return the tile, or {@code null} if the chunk is not currently loaded
     *         or cannot be read.
     */
    Tile readLiveTile(int chunkX, int chunkZ);
    default Tile readLiveTile(String world, int chunkX, int chunkZ) { return readLiveTile(chunkX, chunkZ); }
}
