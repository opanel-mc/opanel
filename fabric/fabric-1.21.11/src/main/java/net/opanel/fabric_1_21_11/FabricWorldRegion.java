package net.opanel.fabric_1_21_11;

import net.minecraft.nbt.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.RegionFile;
import net.minecraft.world.storage.StorageKey;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.fabric_helper.BaseFabricWorldRegion;
import net.opanel.map.Tile;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        StorageKey storageKey = new StorageKey(saveName, World.OVERWORLD, "chunk");
        try(RegionFile regionFile = new RegionFile(storageKey, regionPath, regionPath.getParent(), false)) {
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
            NbtCompound nbt = NbtIo.readCompound(stream);
            NbtCompound heightMaps = nbt.getCompoundOrEmpty("Heightmaps");
            if(heightMaps.isEmpty()) return null;

            Optional<long[]> motionBlockingHeightMapOptional = heightMaps.getLongArray("MOTION_BLOCKING");
            if(motionBlockingHeightMapOptional.isEmpty()) return null;
            long[] motionBlockingHeightMap = motionBlockingHeightMapOptional.get();

            NbtList sectionList = nbt.getListOrEmpty("sections");
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
            int maxY = server.getOverworld().getTopYInclusive() + 1;
            return new Tile(chunkX, chunkZ, sections, motionBlockingHeightMap, minY, maxY);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected Tile.Section readTileSection(NbtCompound sectionNbt) {
        Optional<Byte> yOptional = sectionNbt.getByte("Y");
        if(yOptional.isEmpty()) return null;
        byte y = yOptional.get();

        NbtCompound blockStates = sectionNbt.getCompoundOrEmpty("block_states");
        if(blockStates.isEmpty()) return null;

        NbtCompound biomes = sectionNbt.getCompoundOrEmpty("biomes");
        if(biomes.isEmpty()) return null;

        NbtList paletteNbt = blockStates.getListOrEmpty("palette");
        if(paletteNbt.isEmpty()) return null;
        List<String> palette = new ArrayList<>();
        for(NbtElement item : paletteNbt) {
            if(!(item instanceof NbtCompound)) continue;

            Optional<String> idOptional = ((NbtCompound) item).getString("Name");
            idOptional.ifPresent(palette::add);
        }

        NbtList biomesPaletteNbt = biomes.getListOrEmpty("palette");
        List<String> biomesPalette = new ArrayList<>();
        if(!biomesPaletteNbt.isEmpty()) {
            for(NbtElement biome : biomesPaletteNbt) {
                if(!(biome instanceof NbtString)) continue;
                biomesPalette.add(((NbtString) biome).value());
            }
        }

        Optional<long[]> blockStatesDataOptional = blockStates.getLongArray("data");
        if(blockStatesDataOptional.isEmpty() && palette.size() > 1) return null;
        long[] blockStatesData = blockStatesDataOptional.orElse(new long[] {});
        if(blockStatesData.length == 0 && palette.size() > 1) return null;

        Optional<long[]> biomesDataOptional = biomes.getLongArray("data");
        long[] biomesData = biomesDataOptional.orElse(new long[] {});

        return Tile.createSection(y, palette, blockStatesData, biomesPalette, biomesData);
    }
}
