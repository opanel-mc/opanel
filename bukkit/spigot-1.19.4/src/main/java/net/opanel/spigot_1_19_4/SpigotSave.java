package net.opanel.spigot_1_19_4;

import net.opanel.bukkit_helper.BaseBukkitSave;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelWorldRegion;
import org.bukkit.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SpigotSave extends BaseBukkitSave implements OPanelSave {
    public SpigotSave(Main plugin, Server server, Path path) {
        super(plugin, server, path);
    }

    @Override
    public List<OPanelWorldRegion> getRegions() {
        List<OPanelWorldRegion> regions = new ArrayList<>();

        Path regionFolderPath = savePath.resolve("region");
        if(!regionFolderPath.toFile().exists()) return regions;

        try(Stream<Path> stream = Files.list(regionFolderPath)) {
            stream.filter(path -> (
                            path.toString().endsWith(".mca")
                                    && path.toFile().isFile()
                    ))
                    .map(Path::toAbsolutePath)
                    .forEach(path -> {
                        SpigotWorldRegion region = new SpigotWorldRegion(server, path);
                        regions.add(region);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regions;
    }
}
