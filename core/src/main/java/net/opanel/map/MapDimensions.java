package net.opanel.map;
import java.nio.file.Path;
public final class MapDimensions {
    private MapDimensions() {}
    public static int fromMapKey(String primary, String key) {
        if(key == null || key.equals(primary)) return 0;
        if(key.equals(primary + "_nether")) return -1;
        if(key.equals(primary + "_the_end")) return 1;
        return 2;
    }
    public static java.util.List<net.opanel.common.OPanelWorldRegion> readRegions(Path folder, java.util.function.Function<Path, net.opanel.common.OPanelWorldRegion> factory) {
        if(!java.nio.file.Files.isDirectory(folder)) return java.util.List.of();
        try(var files = java.nio.file.Files.list(folder)) {
            return files.filter(path -> java.nio.file.Files.isRegularFile(path) && path.toString().endsWith(".mca"))
                .sorted().map(Path::toAbsolutePath).map(factory).toList();
        } catch(java.io.IOException e) {
            throw new java.io.UncheckedIOException("Cannot read map regions from " + folder, e);
        }
    }
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
