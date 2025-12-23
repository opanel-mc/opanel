package net.opanel.folia_1_21;

import net.opanel.bukkit_helper.BaseBukkitWhitelist;
import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.io.IOException;
import java.util.*;

public class FoliaWhitelist extends BaseBukkitWhitelist implements OPanelWhitelist {
    public FoliaWhitelist(Main plugin, Server server, Set<OfflinePlayer> playerList) {
        super(plugin, server, playerList);
    }
}