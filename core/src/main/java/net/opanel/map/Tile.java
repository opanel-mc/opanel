package net.opanel.map;

import net.opanel.utils.AnvilUtility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tile {
    private static final String AIR_ID = "minecraft:air";
    private static final String THE_VOID_ID = "minecraft:the_void";
    private static final String PLAINS_ID = "minecraft:plains";
    private static final int HEIGHT_MAP_COLUMNS = 16 * 16;

    public static class Block {
        public final String id;
        public final String biome;

        private Block(String id, String biome) {
            this.id = id;
            this.biome = biome;
        }
    }

    public static class Section {
        private final int y;
        private final List<String> palette;
        private final int[] blockStates;
        private final List<String> biomesPalette;
        private final int[] biomes;

        private Section(int y, List<String> palette, int[] blockStates, List<String> biomesPalette, int[] biomes) {
            this.y = y;
            this.palette = palette;
            this.blockStates = blockStates;
            this.biomesPalette = biomesPalette;
            this.biomes = biomes;
        }

        public int getY() {
            return y;
        }

        /**
         *
         * @param x Relative X in the section
         * @param y Relative Y in the section
         * @param z Relative Z in the section
         * @return Block type
         */
        public String getBlockType(int x, int y, int z) {
            final int index = y * 256 + z * 16 + x;
            if(index >= blockStates.length) {
                return AIR_ID;
            }
            return palette.get(blockStates[index]);
        }

        /**
         *
         * @param x Relative X in the section
         * @param y Relative Y in the section
         * @param z Relative Z in the section
         * @return Biome type
         */
        public String getBlockBiomeType(int x, int y, int z) {
            if(biomesPalette == null || biomesPalette.isEmpty()) return PLAINS_ID;
            if(biomes == null) {
                return biomesPalette.get(0);
            }

            int biomeX = x >>> 2;
            int biomeY = y >>> 2;
            int biomeZ = z >>> 2;
            final int index = biomeY * 16 + biomeZ * 4 + biomeX;
            return biomesPalette.get(biomes[index]);
        }
    }

    private final int chunkX;
    private final int chunkZ;
    private final Map<Integer, Section> sections = new HashMap<>();
    private final int[] heightMap;
    private final int heightMapBits;
    private final int minY;

    /**
     * @param minY inclusive lower world height bound
     * @param maxY exclusive upper world height bound, so the highest valid block Y is {@code maxY - 1}
     */
    public Tile(int chunkX, int chunkZ, List<Section> sections, long[] packedHeightMap, int minY, int maxY) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minY = minY;

        for(Section section : sections) {
            this.sections.put(section.getY(), section);
        }

        heightMapBits = AnvilUtility.heightMapBitsFromYRange(minY, maxY);
        heightMap = Arrays.copyOf(AnvilUtility.bitunpack(packedHeightMap, heightMapBits), HEIGHT_MAP_COLUMNS);
    }

    public int getX() {
        return chunkX;
    }

    public int getZ() {
        return chunkZ;
    }

    /**
     * @return A 16x16 two-dimensional array storing top block type ids
     */
    public Block[][] getTopBlocks() {
        Block[][] result = new Block[16][16];
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int y = getHeight(x, z);
                if(y < minY) {
                    result[z][x] = new Block(AIR_ID, THE_VOID_ID);
                    continue;
                }

                Section section = sections.get(y >> 4);
                if(section == null) {
                    result[z][x] = new Block(AIR_ID, THE_VOID_ID);
                    continue;
                }

                y &= 15;
                result[z][x] = new Block(
                    section.getBlockType(x, y, z),
                    section.getBlockBiomeType(x, y, z)
                );
            }
        }
        return result;
    }

    public int getHeight(int x, int z) {
        int storedHeight = heightMap[z * 16 + x];
        return storedHeight + minY - 1;
    }

    public int[] getHeightMap() {
        return heightMap;
    }

    public int getHeightMapBits() {
        return heightMapBits;
    }

    public static Section createSection(int y, List<String> palette, long[] packedBlockStates) {
        return createSection(y, palette, packedBlockStates, List.of(), null);
    }

    public static Section createSection(int y, List<String> palette, long[] packedBlockStates, List<String> biomesPalette, long[] packedBiomes) {
        int paletteSize = palette.size();
        int biomesPaletteSize = biomesPalette.size();
        return new Section(
            y,
            palette,
            AnvilUtility.bitunpack(packedBlockStates, AnvilUtility.paletteSizeToBitsSize(paletteSize, 4)),
            biomesPalette,
            packedBiomes != null && biomesPaletteSize > 1 ? AnvilUtility.bitunpack(packedBiomes, AnvilUtility.paletteSizeToBitsSize(biomesPaletteSize)) : null
        );
    }
}
