package net.opanel.bukkit_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTList;
import de.tr7zw.changeme.nbtapi.NBTStringList;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTList;
import net.opanel.annotation.Rewrite;
import net.opanel.bukkit_helper.utils.BukkitUtils;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.map.Tile;
import org.bukkit.Server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public abstract class BaseBukkitWorldRegion implements OPanelWorldRegion {
    private static final int SECTOR_BYTES = 4096;

    private static final int COMPRESSION_GZIP = 1;
    private static final int COMPRESSION_ZLIB = 2;
    private static final int COMPRESSION_NONE = 3;
    private static final int EXTERNAL_FLAG = 0x80;

    protected final Server server;
    protected final Path regionPath;
    private RandomAccessFile raf;

    public BaseBukkitWorldRegion(Server server, Path regionPath) {
        this.server = server;

        if(!regionPath.toString().endsWith(".mca")) {
            throw new IllegalArgumentException("Region file extension must be .mca");
        }
        this.regionPath = regionPath;
    }

    @Override
    public Path getPath() {
        return regionPath;
    }

    /**
     * Read the chunk at ({@code chunkX}, {@code chunkZ}) from this MCA region file
     * and return its NBT bytes wrapped in a GZIP stream so Item-NBT-API can parse
     * them. NBT-API's `readNBT` only accepts GZIP-compressed input, while .mca
     * chunks are typically Zlib-compressed; we decompress with the right scheme
     * here and re-wrap in GZIP for the caller. Cost is negligible compared to
     * the NBT parse itself.
     * Must be called while {@link #raf} is open (i.e. from within {@link #getChunkTiles()}).
     *
     * @return GZIP-wrapped NBT InputStream, or {@code null} if the chunk is absent
     */
    protected final InputStream readChunkNBT(int chunkX, int chunkZ) throws IOException {
        if(raf == null) return null;

        // MCA header (8 KiB total):
        //   First 4 KiB: 1024 location entries (3-byte sector offset + 1-byte sector count)
        //   Second 4 KiB: 1024 last-modified timestamps (unused here)
        int headerIndex = 4 * ((chunkX & 31) + (chunkZ & 31) * 32);
        raf.seek(headerIndex);
        int location = raf.readInt();
        int sectorOffset = location >>> 8;
        int sectorCount = location & 0xFF;
        if(sectorOffset == 0 || sectorCount == 0) {
            return null;
        }

        long byteOffset = (long) sectorOffset * SECTOR_BYTES;
        if(byteOffset + 5 > raf.length()) {
            return null;
        }
        raf.seek(byteOffset);

        int chunkLength = raf.readInt();
        int compressionType = raf.readByte() & 0xFF;
        if(chunkLength <= 0) {
            return null;
        }

        boolean external = (compressionType & EXTERNAL_FLAG) != 0;
        int compressionScheme = compressionType & 0x7F;

        byte[] data;
        if(external) {
            Path mccPath = regionPath.resolveSibling("c." + chunkX + "." + chunkZ + ".mcc");
            if(!Files.exists(mccPath)) {
                return null;
            }
            data = Files.readAllBytes(mccPath);
        } else {
            // chunkLength includes the 1 byte for compression type
            int dataLength = chunkLength - 1;
            data = new byte[dataLength];
            raf.readFully(data);
        }

        byte[] rawNbt;
        try(InputStream decompressed = decompress(new ByteArrayInputStream(data), compressionScheme)) {
            rawNbt = decompressed.readAllBytes();
        }

        ByteArrayOutputStream gzipped = new ByteArrayOutputStream(rawNbt.length);
        try(GZIPOutputStream gzipOut = new GZIPOutputStream(gzipped)) {
            gzipOut.write(rawNbt);
        }
        return new ByteArrayInputStream(gzipped.toByteArray());
    }

    private InputStream decompress(InputStream raw, int compressionScheme) throws IOException {
        return switch(compressionScheme) {
            case COMPRESSION_GZIP -> new GZIPInputStream(raw);
            case COMPRESSION_ZLIB -> new InflaterInputStream(raw);
            case COMPRESSION_NONE -> raw;
            default -> {
                raw.close();
                throw new IOException("Unsupported MCA chunk compression scheme: " + compressionScheme);
            }
        };
    }

    @Override
    public List<Tile> getChunkTiles() {
        List<Tile> tiles = new ArrayList<>();
        File mcaFile = regionPath.toFile();
        if(!mcaFile.exists() || mcaFile.length() < SECTOR_BYTES * 2L) {
            return tiles;
        }

        try(RandomAccessFile file = new RandomAccessFile(mcaFile, "r")) {
            raf = file;
            for(int chunkX = 0; chunkX < REGION_SIZE; chunkX++) {
                for(int chunkZ = 0; chunkZ < REGION_SIZE; chunkZ++) {
                    Tile tile = readTile(chunkX, chunkZ);
                    if(tile != null) {
                        tiles.add(tile);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            raf = null;
        }
        return tiles;
    }

    protected Tile readTile(int chunkX, int chunkZ) {
        try(InputStream stream = readChunkNBT(chunkX, chunkZ)) {
            if(stream == null) return null;

            ReadWriteNBT nbt = NBT.readNBT(stream);
            ReadWriteNBT heightMaps = nbt.getCompound("Heightmaps");
            if(heightMaps == null) return null;

            long[] motionBlockingHeightMap = heightMaps.getLongArray("MOTION_BLOCKING");
            if(motionBlockingHeightMap == null || motionBlockingHeightMap.length == 0) return null;

            ReadWriteNBTCompoundList sectionList = nbt.getCompoundList("sections");
            if(sectionList == null || sectionList.isEmpty()) return null;
            List<Tile.Section> sections = new ArrayList<>();
            for(ReadWriteNBT sectionNbt : sectionList) {
                Tile.Section section = readTileSection(sectionNbt);
                if(section != null) {
                    sections.add(section);
                }
            }

            return new Tile(chunkX, chunkZ, sections, motionBlockingHeightMap, BukkitUtils.getMinY(server), BukkitUtils.getMaxY(server));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Tile.Section readTileSection(ReadWriteNBT sectionNbt) {
        byte y = sectionNbt.getByte("Y");

        ReadWriteNBT blockStates = sectionNbt.getCompound("block_states");
        if(blockStates == null) return null;

        ReadWriteNBT biomes = sectionNbt.getCompound("biomes");
        if(biomes == null) return null;

        ReadWriteNBTCompoundList paletteNbt = blockStates.getCompoundList("palette");
        if(paletteNbt == null || paletteNbt.isEmpty()) return null;
        List<String> palette = new ArrayList<>();
        for(ReadWriteNBT item : paletteNbt) {
            String id = item.getString("Name");
            if(id != null) {
                palette.add(id);
            }
        }

        ReadWriteNBTList<String> biomesPaletteNbt = biomes.getStringList("palette");
        List<String> biomesPalette = new ArrayList<>();
        if(biomesPaletteNbt != null && !biomesPaletteNbt.isEmpty()) {
            for(String biome : biomesPaletteNbt) {
                biomesPalette.add(biome);
            }
        }

        long[] blockStatesData = blockStates.getLongArray("data");
        if((blockStatesData == null || blockStatesData.length == 0) && palette.size() > 1) return null;

        long[] biomesData = biomes.getLongArray("data");

        return Tile.createSection(y, palette, blockStatesData, biomesPalette, biomesData);
    }
}
