package net.opanel.fabric_1_19;

import net.minecraft.nbt.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.RegionFile;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.fabric_helper.BaseFabricWorldRegion;
import net.opanel.map.Tile;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricWorldRegion extends BaseFabricWorldRegion implements OPanelWorldRegion {
    public FabricWorldRegion(net.minecraft.server.MinecraftServer server, String saveName, Path regionPath) {
        super(server, saveName, regionPath);
    }

    @Override
    public List<Tile> getChunkTiles() {
        List<Tile> tiles = new ArrayList<>();
        File mcaFile = regionPath.toFile();
        if(!mcaFile.exists()) {
            return tiles;
        }

        try(RegionFile regionFile = new RegionFile(regionPath, regionPath.getParent(), false)) {
            for(int chunkX = 0; chunkX < REGION_SIZE; chunkX++) {
                for(int chunkZ = 0; chunkZ < REGION_SIZE; chunkZ++) {
                    DataInputStream dis = regionFile.getChunkInputStream(new ChunkPos(chunkX, chunkZ));
                    if(dis == null) continue;

                    Tile tile = readTile(chunkX, chunkZ, dis);
                    if(tile == null) continue;

                    tiles.add(tile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tiles;
    }

    @Override
    protected Tile readTile(int chunkX, int chunkZ, DataInputStream stream) {
        try {
            NbtCompound nbt = NbtIo.read(stream);
            NbtCompound heightMaps = nbt.getCompound("Heightmaps");
            if(heightMaps == null || heightMaps.isEmpty()) return null;

            long[] motionBlockingHeightMap = heightMaps.getLongArray("MOTION_BLOCKING");
            if(motionBlockingHeightMap == null || motionBlockingHeightMap.length == 0) return null;

            NbtList sectionList = nbt.getList("sections", NbtElement.COMPOUND_TYPE);
            if(sectionList.isEmpty()) return null;
            List<Tile.Section> sections = new ArrayList<>();
            for(NbtElement sectionNbt : sectionList) {
                if(!(sectionNbt instanceof NbtCompound)) continue;

                Tile.Section section = readTileSection((NbtCompound) sectionNbt);
                if(section != null) {
                    sections.add(section);
                }
            }

            int minY = server.getOverworld().getBottomY();
            int maxY = server.getOverworld().getTopY();
            return new Tile(chunkX, chunkZ, sections, motionBlockingHeightMap, minY, maxY);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected Tile.Section readTileSection(NbtCompound sectionNbt) {
        byte y = sectionNbt.getByte("Y");

        NbtCompound blockStates = sectionNbt.getCompound("block_states");
        if(blockStates == null || blockStates.isEmpty()) return null;

        NbtCompound biomes = sectionNbt.getCompound("biomes");
        if(biomes == null || biomes.isEmpty()) return null;

        NbtList paletteNbt = blockStates.getList("palette", NbtElement.COMPOUND_TYPE);
        if(paletteNbt.isEmpty()) return null;
        List<String> palette = new ArrayList<>();
        for(NbtElement item : paletteNbt) {
            if(!(item instanceof NbtCompound)) continue;

            String id = ((NbtCompound) item).getString("Name");
            if(id != null) {
                palette.add(id);
            }
        }

        NbtList biomesPaletteNbt = biomes.getList("palette", NbtElement.STRING_TYPE);
        List<String> biomesPalette = new ArrayList<>();
        if(!biomesPaletteNbt.isEmpty()) {
            for(NbtElement biome : biomesPaletteNbt) {
                if(!(biome instanceof NbtString)) continue;
                biomesPalette.add(biome.asString());
            }
        }

        long[] blockStatesData = blockStates.getLongArray("data");
        if((blockStatesData == null || blockStatesData.length == 0) && palette.size() > 1) return null;

        long[] biomesData = biomes.getLongArray("data");

        return Tile.createSection(y, palette, blockStatesData, biomesPalette, biomesData);
    }
}
