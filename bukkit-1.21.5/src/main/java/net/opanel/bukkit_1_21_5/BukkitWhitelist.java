package net.opanel.bukkit_1_21_5;

import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.*;

public class BukkitWhitelist implements OPanelWhitelist {
    private final Server server;
    private final Set<OfflinePlayer> whitelist;

    public BukkitWhitelist(Server server, Set<OfflinePlayer> playerList) {
        this.server = server;
        whitelist = playerList;
    }

    @Override
    public List<String> getNames() {
        List<String> list = new ArrayList<>();
        for(OfflinePlayer player : whitelist) {
            list.add(player.getName());
        }
        return list;
    }

    @Override
    public List<OPanelWhitelistEntry> getEntries() {
        List<OPanelWhitelistEntry> entries = new ArrayList<>();
        for(OfflinePlayer player : whitelist) {
            entries.add(new OPanelWhitelistEntry(player.getName(), player.getUniqueId().toString()));
        }
        return entries;
    }

    /** @todo */
    @Override
    public void write(List<OPanelWhitelistEntry> entries) {

    }

    /** @todo */
    @Override
    public void add(OPanelWhitelistEntry entry) {

    }

    /** @todo */
    @Override
    public void remove(OPanelWhitelistEntry entry) {

    }
}
