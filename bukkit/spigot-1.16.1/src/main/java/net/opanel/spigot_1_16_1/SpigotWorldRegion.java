package net.opanel.spigot_1_16_1;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import net.opanel.annotation.Rewrite;
import net.opanel.bukkit_helper.BaseBukkitWorldRegion;
import net.opanel.bukkit_helper.utils.BukkitUtils;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.map.Tile;
import org.bukkit.Server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SpigotWorldRegion extends BaseBukkitWorldRegion implements OPanelWorldRegion {
    public SpigotWorldRegion(Server server, Path regionPath) {
        super(server, regionPath);
    }

    @Rewrite
    @Override
    protected Tile readTile(int chunkX, int chunkZ) {
        try(InputStream stream = readChunkNBT(chunkX, chunkZ)) {
            if(stream == null) return null;

            ReadWriteNBT nbt = NBT.readNBT(stream).getCompound("Level");
            if(nbt == null) return null;

            ReadWriteNBT heightMaps = nbt.getCompound("Heightmaps");
            if(heightMaps == null) return null;

            long[] motionBlockingHeightMap = heightMaps.getLongArray("MOTION_BLOCKING");
            if(motionBlockingHeightMap == null || motionBlockingHeightMap.length == 0) return null;

            ReadWriteNBTCompoundList sectionList = nbt.getCompoundList("Sections");
            if(sectionList == null || sectionList.isEmpty()) return null;
            List<Tile.Section> sections = new ArrayList<>();
            for(ReadWriteNBT sectionNbt : sectionList) {
                Tile.Section section = readTileSection(sectionNbt);
                if(section != null) {
                    sections.add(section);
                }
            }

            return new Tile(chunkX, chunkZ, sections, motionBlockingHeightMap, 0, BukkitUtils.getMaxY(server));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Rewrite
    @Override
    protected Tile.Section readTileSection(ReadWriteNBT sectionNbt) {
        byte y = sectionNbt.getByte("Y");

        ReadWriteNBTCompoundList paletteNbt = sectionNbt.getCompoundList("Palette");
        if(paletteNbt == null || paletteNbt.isEmpty()) return null;
        List<String> palette = new ArrayList<>();
        for(ReadWriteNBT item : paletteNbt) {
            String id = item.getString("Name");
            if(id != null) {
                palette.add(id);
            }
        }

        long[] blockStates = sectionNbt.getLongArray("BlockStates");
        if((blockStates == null || blockStates.length == 0) && palette.size() > 1) return null;

        return Tile.createSection(y, palette, blockStates);
    }
}
