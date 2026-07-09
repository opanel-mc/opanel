package net.opanel.spigot_1_16_1;

import net.opanel.annotation.Rewrite;
import net.opanel.bukkit_helper.BaseBukkitChunkAccessor;
import net.opanel.bukkit_helper.utils.BukkitUtils;
import net.opanel.map.Tile;
import net.opanel.utils.AnvilUtility;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpigotChunkAccessor extends BaseBukkitChunkAccessor {
    public SpigotChunkAccessor(Main plugin) {
        super(plugin);
    }

    @Rewrite
    @Override
    protected Tile readOnMainThread(int chunkX, int chunkZ) {
        World world = plugin.getServer().getWorlds().get(0);
        if(world == null) return null;
        // OPanel renders only the overworld; non-NORMAL dimensions are out of scope.
        if(world.getEnvironment() != World.Environment.NORMAL) return null;
        if(!world.isChunkLoaded(chunkX, chunkZ)) return null;

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        ChunkSnapshot snap = chunk.getChunkSnapshot(true, true, false);

        final int minY = 0;
        final int maxY = BukkitUtils.getMaxY(world);
        final int firstSection = minY >> 4;
        final int lastSection = (maxY - 1) >> 4;
        final int sectionCount = lastSection - firstSection + 1;
        final int heightMapBits = AnvilUtility.heightMapBitsFromYRange(minY, maxY);
        final int maxStoredHeight = maxY - minY;

        List<Tile.Section> sections = new ArrayList<>(sectionCount);
        for(int sectionY = firstSection; sectionY <= lastSection; sectionY++) {
            Tile.Section section = buildSection(snap, sectionY);
            if(section != null) sections.add(section);
        }

        int[] heightMap = new int[256];
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int highest = snap.getHighestBlockYAt(x, z);
                // Tile.getHeight() returns storedHeight + minY, so invert.
                int stored = highest - minY;
                if(stored < 0) stored = 0;
                if(stored > maxStoredHeight) stored = maxStoredHeight;
                heightMap[z * 16 + x] = stored;
            }
        }
        long[] packedHeightMap = AnvilUtility.bitpack(heightMap, heightMapBits);

        return new Tile(chunkX, chunkZ, sections, packedHeightMap, minY, maxY);
    }

    @Rewrite
    @Override
    protected Tile.Section buildSection(ChunkSnapshot snap, int sectionY) {
        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        int[] blockStates = new int[16 * 16 * 16];
        for(int y = 0; y < 16; y++) {
            int worldY = sectionY * 16 + y;
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    BlockData data = snap.getBlockData(x, worldY, z);
                    String id = data.getMaterial().getKey().toString();
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
        // OPanel map in 1.16.x doesn't support multi-biome rendering,
        // just render the plains biome
        List<String> biomesPalette = new ArrayList<>();
        biomesPalette.add(FALLBACK_BIOME);

        int[] biomes = new int[64];
        for(int by = 0; by < 4; by++) {
            for(int bz = 0; bz < 4; bz++) {
                for(int bx = 0; bx < 4; bx++) {
                    biomes[by * 16 + bz * 4 + bx] = 0;
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
