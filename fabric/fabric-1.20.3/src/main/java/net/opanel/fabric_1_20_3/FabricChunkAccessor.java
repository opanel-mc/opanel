package net.opanel.fabric_1_20_3;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import net.opanel.common.OPanelChunkAccessor;
import net.opanel.fabric_helper.BaseFabricChunkAccessor;
import net.opanel.map.Tile;
import net.opanel.utils.AnvilUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FabricChunkAccessor extends BaseFabricChunkAccessor implements OPanelChunkAccessor {
    public FabricChunkAccessor(MinecraftServer server) {
        super(server);
    }

    @Override
    protected Tile readOnMainThread(int chunkX, int chunkZ) {
        ServerWorld world = server.getOverworld();
        WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
        if(chunk == null) return null;

        final int minY = -64;
        final int maxY = world.getTopY();
        final int firstSection = minY >> 4;
        final int lastSection = (maxY - 1) >> 4;
        final int sectionCount = lastSection - firstSection + 1;
        final int heightMapBits = AnvilUtility.heightMapBitsFromSectionCount(sectionCount);
        final int maxStoredHeight = AnvilUtility.maxStoredHeightFromSectionCount(sectionCount);

        List<Tile.Section> sections = new ArrayList<>(sectionCount);
        for(int sectionY = firstSection; sectionY <= lastSection; sectionY++) {
            Tile.Section section = buildSection(chunk, sectionY);
            if(section != null) sections.add(section);
        }

        int[] heightMap = new int[256];
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int highest = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x, z);
                // Tile.getHeight() returns storedHeight + minY - 1, so invert.
                int stored = highest + 1 - minY;
                if(stored < 0) stored = 0;
                if(stored > maxStoredHeight) stored = maxStoredHeight;
                heightMap[z * 16 + x] = stored;
            }
        }
        long[] packedHeightMap = AnvilUtility.bitpack(heightMap, heightMapBits);

        return new Tile(chunkX, chunkZ, sections, packedHeightMap, true);
    }

    @Override
    protected Tile.Section buildSection(WorldChunk chunk, int sectionY) {
        ChunkPos chunkPos = chunk.getPos();

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        int[] blockStates = new int[16 * 16 * 16];
        for(int y = 0; y < 16; y++) {
            int worldY = sectionY * 16 + y;
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    BlockState data = chunk.getBlockState(new BlockPos(x, worldY, z));
                    String id = Registries.BLOCK.getId(data.getBlock()).toString();
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
                    biomeKey = chunk.getWorld().getBiome(new BlockPos((chunkPos.x << 4) + bx * 4, worldY, (chunkPos.z << 4) + bz * 4))
                            .getKey()
                            .map(RegistryKey::getValue)
                            .map(Identifier::toString)
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
