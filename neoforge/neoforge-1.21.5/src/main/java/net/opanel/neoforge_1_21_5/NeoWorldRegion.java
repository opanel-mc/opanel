package net.opanel.neoforge_1_21_5;

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
import java.util.Optional;

public class NeoWorldRegion extends BaseNeoWorldRegion implements OPanelWorldRegion {
    public NeoWorldRegion(MinecraftServer server, String saveName, Path regionPath) {
        super(server, saveName, regionPath);
    }

    @Override
    protected Tile readTile(int chunkX, int chunkZ, DataInputStream stream) {
        try {
            CompoundTag nbt = NbtIo.read(stream);
            CompoundTag heightMaps = nbt.getCompoundOrEmpty("Heightmaps");
            if(heightMaps.isEmpty()) return null;

            Optional<long[]> motionBlockingHeightMapOptional = heightMaps.getLongArray("MOTION_BLOCKING");
            if(motionBlockingHeightMapOptional.isEmpty()) return null;
            long[] motionBlockingHeightMap = motionBlockingHeightMapOptional.get();

            ListTag sectionList = nbt.getListOrEmpty("sections");
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
        Optional<Byte> yOptional = sectionNbt.getByte("Y");
        if(yOptional.isEmpty()) return null;
        byte y = yOptional.get();

        CompoundTag blockStates = sectionNbt.getCompoundOrEmpty("block_states");
        if(blockStates.isEmpty()) return null;

        CompoundTag biomes = sectionNbt.getCompoundOrEmpty("biomes");
        if(biomes.isEmpty()) return null;

        ListTag paletteNbt = blockStates.getListOrEmpty("palette");
        if(paletteNbt.isEmpty()) return null;
        List<String> palette = new ArrayList<>();
        for(Tag item : paletteNbt) {
            if(!(item instanceof CompoundTag)) continue;

            Optional<String> idOptional = ((CompoundTag) item).getString("Name");
            idOptional.ifPresent(palette::add);
        }

        ListTag biomesPaletteNbt = biomes.getListOrEmpty("palette");
        List<String> biomesPalette = new ArrayList<>();
        if(!biomesPaletteNbt.isEmpty()) {
            for(Tag biome : biomesPaletteNbt) {
                if(!(biome instanceof StringTag)) continue;
                biomesPalette.add(((StringTag) biome).value());
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
