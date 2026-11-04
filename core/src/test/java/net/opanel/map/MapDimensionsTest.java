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
    @Test public void firstWorldMapDoesNotRequireLevelDat() throws Exception {
        Path root = java.nio.file.Files.createTempDirectory("opanel-fresh-map");
        java.nio.file.Files.createDirectories(root.resolve("region"));
        java.nio.file.Files.write(root.resolve("region/r.0.0.mca"), new byte[0]);
        net.opanel.common.OPanelServer server = (net.opanel.common.OPanelServer) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{net.opanel.common.OPanelServer.class}, (proxy, method, args) -> {
            if(method.getName().equals("getMapRootPath")) return root;
            if(method.getName().equals("getCurrentSaveName")) return "world";
            if(method.getName().equals("isMapDimensionLoaded")) return true;
            if(method.getName().equals("getMapRegions")) return MapDimensions.readRegions((Path)args[0], path -> new net.opanel.common.OPanelWorldRegion() {
                public Path getPath() { return path; }
                public java.util.List<Tile> getChunkTiles() { return java.util.List.of(); }
            });
            if(method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
            throw new AssertionError("Unexpected metadata dependency: " + method.getName());
        });
        try {
            assertFalse(java.nio.file.Files.exists(root.resolve("level.dat")));
            var worlds = server.getMapWorlds();
            assertEquals(java.util.Set.of("world", "world_nether", "world_the_end"), worlds.keySet());
            assertEquals(1, worlds.get("world").size());
        } finally {
            java.nio.file.Files.deleteIfExists(root.resolve("region/r.0.0.mca"));
            java.nio.file.Files.deleteIfExists(root.resolve("region"));
            java.nio.file.Files.deleteIfExists(root);
        }
    }
}
