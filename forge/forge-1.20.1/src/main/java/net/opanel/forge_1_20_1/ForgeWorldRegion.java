package net.opanel.forge_1_20_1;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.forge_helper.BaseForgeWorldRegion;
import net.opanel.map.Tile;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ForgeWorldRegion extends BaseForgeWorldRegion implements OPanelWorldRegion {
    public ForgeWorldRegion(MinecraftServer server, String saveName, Path regionPath) {
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
                    DataInputStream dis = regionFile.getChunkDataInputStream(new ChunkPos(chunkX, chunkZ));
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
            CompoundTag nbt = NbtIo.read(stream);
            CompoundTag heightMaps = nbt.getCompound("Heightmaps");
            if(heightMaps.isEmpty()) return null;

            long[] motionBlockingHeightMap = heightMaps.getLongArray("MOTION_BLOCKING");
            if(motionBlockingHeightMap.length == 0) return null;

            ListTag sectionList = nbt.getList("sections", ListTag.TAG_COMPOUND);
            if(sectionList.isEmpty()) return null;
            List<Tile.Section> sections = new ArrayList<>();
            for(Tag sectionNbt : sectionList) {
                if(!(sectionNbt instanceof CompoundTag)) continue;

                Tile.Section section = readTileSection((CompoundTag) sectionNbt);
                if(section != null) {
                    sections.add(section);
                }
            }

            int minY = server.overworld().getMinBuildHeight();
            int maxY = server.overworld().getMaxBuildHeight();
            return new Tile(chunkX, chunkZ, sections, motionBlockingHeightMap, minY, maxY);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected Tile.Section readTileSection(CompoundTag sectionNbt) {
        byte y = sectionNbt.getByte("Y");

        CompoundTag blockStates = sectionNbt.getCompound("block_states");
        if(blockStates.isEmpty()) return null;

        CompoundTag biomes = sectionNbt.getCompound("biomes");
        if(biomes.isEmpty()) return null;

        ListTag paletteNbt = blockStates.getList("palette", ListTag.TAG_COMPOUND);
        if(paletteNbt.isEmpty()) return null;
        List<String> palette = new ArrayList<>();
        for(Tag item : paletteNbt) {
            if(!(item instanceof CompoundTag)) continue;

            String id = ((CompoundTag) item).getString("Name");
            if(!id.isEmpty()) {
                palette.add(id);
            }
        }

        ListTag biomesPaletteNbt = biomes.getList("palette", ListTag.TAG_STRING);
        List<String> biomesPalette = new ArrayList<>();
        if(!biomesPaletteNbt.isEmpty()) {
            for(Tag biome : biomesPaletteNbt) {
                if(!(biome instanceof StringTag)) continue;
                biomesPalette.add(biome.getAsString());
            }
        }

        long[] blockStatesData = blockStates.getLongArray("data");
        if(blockStatesData.length == 0 && palette.size() > 1) return null;

        long[] biomesData = biomes.getLongArray("data");

        return Tile.createSection(y, palette, blockStatesData, biomesPalette, biomesData);
    }
}
