package net.opanel.paper_1_21_9;

import net.opanel.paper_helper.BasePaperWhitelist;
import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.*;

public class PaperWhitelist extends BasePaperWhitelist implements OPanelWhitelist {
    public PaperWhitelist(Main plugin, Server server, Set<OfflinePlayer> playerList) {
        super(plugin, server, playerList);
    }
}
