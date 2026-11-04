package net.opanel.map;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.Path;
public class MapDimensionsTest {
    @Test public void standardDimensionLayoutsResolveWithoutLoadingWorlds() {
        assertEquals(-1, MapDimensions.fromRegionPath(Path.of("world/DIM-1/region/r.0.0.mca")));
        assertEquals(1, MapDimensions.fromRegionPath(Path.of("world/DIM1/region/r.0.0.mca")));
        assertEquals(-1, MapDimensions.fromRegionPath(Path.of("world/dimensions/minecraft/the_nether/region/r.0.0.mca")));
        assertEquals(1, MapDimensions.fromRegionPath(Path.of("world/dimensions/minecraft/the_end/region/r.0.0.mca")));
        assertEquals(0, MapDimensions.fromRegionPath(Path.of("backups/the_nether/world/region/r.0.0.mca")));
    }
}
