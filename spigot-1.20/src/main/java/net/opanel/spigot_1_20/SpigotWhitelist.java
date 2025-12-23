package net.opanel.spigot_1_20;

import net.opanel.bukkit_helper.BaseBukkitWhitelist;
import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.*;

public class SpigotWhitelist extends BaseBukkitWhitelist implements OPanelWhitelist {
    public SpigotWhitelist(Main plugin, Server server, Set<OfflinePlayer> playerList) {
        super(plugin, server, playerList);
    }
}
