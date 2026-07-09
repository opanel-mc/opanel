package net.opanel.neoforge_1_21_3;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.map.Tile;
import net.opanel.neoforge_helper.BaseNeoWorldRegion;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NeoWorldRegion extends BaseNeoWorldRegion implements OPanelWorldRegion {
    public NeoWorldRegion(MinecraftServer server, String saveName, Path regionPath) {
        super(server, saveName, regionPath);
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

            int minY = server.overworld().getMinY();
            int maxY = server.overworld().getMaxY() + 1;
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
