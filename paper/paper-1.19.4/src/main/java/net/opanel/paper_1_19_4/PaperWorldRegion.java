package net.opanel.paper_1_19_4;

import net.opanel.paper_helper.BasePaperWorldRegion;
import net.opanel.common.OPanelWorldRegion;
import org.bukkit.Server;

import java.nio.file.Path;

public class PaperWorldRegion extends BasePaperWorldRegion implements OPanelWorldRegion {
    public PaperWorldRegion(Server server, Path regionPath) {
        super(server, regionPath);
    }
}
