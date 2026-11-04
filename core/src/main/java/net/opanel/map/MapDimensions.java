package net.opanel.map;
import java.nio.file.Path;
public final class MapDimensions {
    private MapDimensions() {}
    public static int fromRegionPath(Path path) {
        Path regionFolder = path.getParent();
        Path dimensionFolder = regionFolder == null ? null : regionFolder.getParent();
        if(dimensionFolder == null || dimensionFolder.getFileName() == null) return 0;
        String name = dimensionFolder.getFileName().toString();
        if(name.equals("DIM-1") || name.equals("the_nether")) return -1;
        if(name.equals("DIM1") || name.equals("the_end")) return 1;
        return 0;
    }
}
