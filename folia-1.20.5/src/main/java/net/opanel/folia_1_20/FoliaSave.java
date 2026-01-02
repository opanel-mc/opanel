package net.opanel.folia_1_20;

import net.opanel.bukkit_helper.BaseBukkitSave;
import net.opanel.common.OPanelSave;
import org.bukkit.Server;

import java.nio.file.Path;

public class FoliaSave extends BaseBukkitSave implements OPanelSave {
    public FoliaSave(Main plugin, Server server, Path path) {
        super(plugin, server, path);
    }
}
