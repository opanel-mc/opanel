package net.opanel.map;

import net.opanel.utils.AnvilUtility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileCompressor {
    private static final byte[] TILE_DATA_MAGIC = "OTILE".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] BUNDLED_TILES_DATA_MAGIC = "OTILES".getBytes(StandardCharsets.US_ASCII);

    public static ByteArrayOutputStream compressTile(Tile tile) throws IOException {
        Tile.Block[][] topBlocks = tile.getTopBlocks();
        int[] heightMap = tile.getHeightMap();

        // generate palettes
        List<String> palette = new ArrayList<>();
        HashMap<String, Integer> indexes = new HashMap<>();
        List<String> biomesPalette = new ArrayList<>();
        HashMap<String, Integer> biomesIndexes = new HashMap<>();
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                Tile.Block block = topBlocks[z][x];

                String id = block.id;
                if(!indexes.containsKey(id)) {
                    palette.add(id);
                    indexes.put(id, palette.size() - 1);
                }

                String biome = block.biome;
                if(!biomesIndexes.containsKey(biome)) {
                    biomesPalette.add(biome);
                    biomesIndexes.put(biome, biomesPalette.size() - 1);
                }
            }
        }

        // generate block data and biomes data
        int[] blockData = new int[256];
        int[] biomesData = new int[256];
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int index = z * 16 + x;
                Tile.Block block = topBlocks[z][x];
                blockData[index] = indexes.get(block.id);
                biomesData[index] = biomesIndexes.get(block.biome);
            }
        }
        long[] bitpackedBlockData = AnvilUtility.bitpack(blockData, AnvilUtility.paletteSizeToBitsSize(palette.size(), 4));
        long[] bitpackedBiomesData = biomesPalette.size() > 1 ? AnvilUtility.bitpack(biomesData, AnvilUtility.paletteSizeToBitsSize(biomesPalette.size())) : new long[] { 0L };

        // pack height map
        long[] bitpackedHeightMap = AnvilUtility.bitpack(heightMap, tile.getHeightMapBits());

        // start writing to output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write(TILE_DATA_MAGIC);

        // write palette part
        dos.writeShort(palette.size());
        for(String id : palette) {
            byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
            dos.writeByte(idBytes.length & 0xff);
            dos.write(idBytes);
        }

        // write block data part
        dos.writeShort(bitpackedBlockData.length);
        for(long data : bitpackedBlockData) {
            dos.writeLong(data);
        }

        // write height map part
        dos.writeShort(bitpackedHeightMap.length);
        for(long data : bitpackedHeightMap) {
            dos.writeLong(data);
        }

        // write biomes palette part
        dos.writeShort(biomesPalette.size());
        for(String biome : biomesPalette) {
            byte[] biomeBytes = biome.getBytes(StandardCharsets.UTF_8);
            dos.writeByte(biomeBytes.length & 0xff);
            dos.write(biomeBytes);
        }

        // write biomes data part
        dos.writeShort(bitpackedBiomesData.length);
        for(long data : bitpackedBiomesData) {
            dos.writeLong(data);
        }

        dos.flush();
        return baos;
    }

    public static ByteArrayOutputStream bundleTiles(Map<Long, byte[]> tiles) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(BUNDLED_TILES_DATA_MAGIC);
        dos.writeInt(tiles.size());
        for(Map.Entry<Long, byte[]> entry : tiles.entrySet()) {
            Long packedCoord = entry.getKey();
            byte[] bytes = entry.getValue();
            dos.writeLong(packedCoord);
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }

        dos.flush();
        return baos;
    }

    public static Map<Long, byte[]> parseBundle(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        byte[] magic = new byte[BUNDLED_TILES_DATA_MAGIC.length];
        dis.readFully(magic);
        if(!Arrays.equals(magic, BUNDLED_TILES_DATA_MAGIC)) {
            throw new IOException("Bad otiles magic");
        }

        int count = dis.readInt();
        HashMap<Long, byte[]> tiles = new HashMap<>(count * 4 / 3 + 1);
        for(int i = 0; i < count; i++) {
            long packed = dis.readLong();
            int len = dis.readInt();
            byte[] bytes = new byte[len];
            dis.readFully(bytes);
            tiles.put(packed, bytes);
        }
        return tiles;
    }
}
