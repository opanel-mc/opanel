package net.opanel.map;
import java.util.*;
import net.opanel.utils.AnvilUtility;
import org.junit.Test;
import static org.junit.Assert.*;
public class NetherSurfaceTest {
    @Test public void ceilingIsSkippedAndGroundIsPreserved() {
        int[] heights = new int[256];
        Arrays.fill(heights, 128);
        Tile tile = new Tile(0, 0, List.of(
            Tile.createSection(7, List.of("minecraft:bedrock"), null),
            Tile.createSection(3, List.of("minecraft:netherrack"), null)
        ), AnvilUtility.bitpack(heights, AnvilUtility.heightMapBitsFromYRange(0, 256)), 0, 256);
        tile.useNetherSurface();
        tile.resolveSurfaceHeights();
        assertEquals(63, tile.getHeight(0, 0));
        assertEquals("minecraft:netherrack", tile.getTopBlocks()[0][0].id());
    }
    @Test public void emptyNetherStaysTransparent() {
        int[] heights = new int[256];
        Tile tile = new Tile(0, 0, List.of(), AnvilUtility.bitpack(heights, 9), 0, 256);
        tile.useNetherSurface();
        tile.resolveSurfaceHeights();
        assertEquals(-1, tile.getHeight(0, 0));
    }
}
