package net.opanel.neoforge_1_21_3;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.opanel.common.OPanelChunkAccessor;
import net.opanel.map.Tile;
import net.opanel.neoforge_helper.BaseNeoChunkAccessor;
import net.opanel.utils.AnvilUtility;

import java.util.ArrayList;
import java.util.List;

public class NeoChunkAccessor extends BaseNeoChunkAccessor implements OPanelChunkAccessor {
    public NeoChunkAccessor(MinecraftServer server) {
        super(server);
    }

    @Override
    protected Tile readOnMainThread(int chunkX, int chunkZ) {
        ServerLevel world = server.overworld();
        LevelChunk chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
        if(chunk == null) return null;

        final int minY = -64;
        final int maxY = world.getMaxY();
        final int firstSection = minY >> 4;
        final int lastSection = (maxY - 1) >> 4;
        final int sectionCount = lastSection - firstSection + 1;
        final int heightMapBits = AnvilUtility.heightMapBitsFromSectionCount(sectionCount);
        final int maxStoredHeight = AnvilUtility.maxStoredHeightFromSectionCount(sectionCount);

        List<Tile.Section> sections = new ArrayList<>(sectionCount);
        for(int sectionY = firstSection; sectionY <= lastSection; sectionY++) {
            Tile.Section section = buildSection(chunk, sectionY);
            if(section != null) sections.add(section);
        }

        int[] heightMap = new int[256];
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int highest = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                // Tile.getHeight() returns storedHeight + minY - 1, so invert.
                int stored = highest + 1 - minY;
                if(stored < 0) stored = 0;
                if(stored > maxStoredHeight) stored = maxStoredHeight;
                heightMap[z * 16 + x] = stored;
            }
        }
        long[] packedHeightMap = AnvilUtility.bitpack(heightMap, heightMapBits);

        return new Tile(chunkX, chunkZ, sections, packedHeightMap, true);
    }
}
