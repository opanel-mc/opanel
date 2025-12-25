package net.opanel.spigot_1_20_6;

import net.opanel.bukkit_helper.BaseBukkitSave;
import net.opanel.common.OPanelSave;
import org.bukkit.Server;

import java.nio.file.Path;

public class SpigotSave extends BaseBukkitSave implements OPanelSave {
    public SpigotSave(Main plugin, Server server, Path path) {
        super(plugin, server, path);
    }
}
