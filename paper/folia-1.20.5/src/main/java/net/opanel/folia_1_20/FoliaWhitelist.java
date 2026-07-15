package net.opanel.folia_1_20;

import net.opanel.paper_helper.BasePaperWhitelist;
import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.io.IOException;
import java.util.*;

public class FoliaWhitelist extends BasePaperWhitelist implements OPanelWhitelist {
    public FoliaWhitelist(Main plugin, Server server, Set<OfflinePlayer> playerList) {
        super(plugin, server, playerList);
    }
}