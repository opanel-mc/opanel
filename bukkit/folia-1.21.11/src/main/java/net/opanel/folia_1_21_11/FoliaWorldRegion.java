package net.opanel.folia_1_21_11;

import net.opanel.bukkit_helper.BaseBukkitWorldRegion;
import net.opanel.common.OPanelWorldRegion;
import org.bukkit.Server;

import java.nio.file.Path;

public class FoliaWorldRegion extends BaseBukkitWorldRegion implements OPanelWorldRegion {
    public FoliaWorldRegion(Server server, Path regionPath) {
        super(server, regionPath);
    }
}
