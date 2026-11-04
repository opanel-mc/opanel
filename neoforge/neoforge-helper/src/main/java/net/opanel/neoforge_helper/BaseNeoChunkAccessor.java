package net.opanel.neoforge_helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.opanel.common.OPanelChunkAccessor;
import net.opanel.map.Tile;
import net.opanel.utils.AnvilUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class BaseNeoChunkAccessor implements OPanelChunkAccessor {
    protected final MinecraftServer server;

    public BaseNeoChunkAccessor(MinecraftServer server) {
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

    protected Tile.Section buildSection(LevelChunk chunk, int sectionY) {
        ChunkPos chunkPos = chunk.getPos();

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        int[] blockStates = new int[16 * 16 * 16];
        for(int y = 0; y < 16; y++) {
            int worldY = sectionY * 16 + y;
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    BlockState data = chunk.getBlockState(new BlockPos(x, worldY, z));
                    ResourceLocation resource = BuiltInRegistries.BLOCK.getKey(data.getBlock());

                    String id = resource.toString();
                    Integer idx = paletteIndex.get(id);
                    if(idx == null) {
                        idx = palette.size();
                        palette.add(id);
                        paletteIndex.put(id, idx);
                    }
                    blockStates[y * 256 + z * 16 + x] = idx;
                }
            }
        }
        int blockBits = AnvilUtility.paletteSizeToBitsSize(palette.size(), 4);
        long[] packedBlockStates = AnvilUtility.bitpack(blockStates, blockBits);

        // Biomes are stored on a 4×4×4 grid (64 cells per section).
        List<String> biomesPalette = new ArrayList<>();
        Map<String, Integer> biomesIndex = new HashMap<>();
        int[] biomes = new int[64];
        for(int by = 0; by < 4; by++) {
            int worldY = sectionY * 16 + by * 4;
            for(int bz = 0; bz < 4; bz++) {
                for(int bx = 0; bx < 4; bx++) {
                    String biomeKey;
                    biomeKey = chunk.getLevel().getBiome(new BlockPos((chunkPos.x << 4) + bx * 4, worldY, (chunkPos.z << 4) + bz * 4))
                            .unwrapKey()
                            .map(ResourceKey::location)
                            .map(ResourceLocation::toString)
                            .orElse(FALLBACK_BIOME);
                    Integer idx = biomesIndex.get(biomeKey);
                    if(idx == null) {
                        idx = biomesPalette.size();
                        biomesPalette.add(biomeKey);
                        biomesIndex.put(biomeKey, idx);
                    }
                    biomes[by * 16 + bz * 4 + bx] = idx;
                }
            }
        }

        long[] packedBiomes = null;
        if(biomesPalette.size() > 1) {
            int biomesBits = AnvilUtility.paletteSizeToBitsSize(biomesPalette.size());
            packedBiomes = AnvilUtility.bitpack(biomes, biomesBits);
        }

        return Tile.createSection(sectionY, palette, packedBlockStates, biomesPalette, packedBiomes);
    }
}
