package net.opanel.folia_26_1_2;

import net.opanel.paper_helper.BasePaperWorldRegion;
import net.opanel.common.OPanelWorldRegion;
import org.bukkit.Server;

import java.nio.file.Path;

public class FoliaWorldRegion extends BasePaperWorldRegion implements OPanelWorldRegion {
    public FoliaWorldRegion(Server server, Path regionPath) {
        super(server, regionPath);
    }
}
