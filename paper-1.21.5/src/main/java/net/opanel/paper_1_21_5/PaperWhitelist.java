package net.opanel.paper_1_21_5;

import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.*;

public class PaperWhitelist implements OPanelWhitelist {
    private final Server server;
    private final Set<OfflinePlayer> whitelist;

    public PaperWhitelist(Server server, Set<OfflinePlayer> playerList) {
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

    @Override
    public void write(List<OPanelWhitelistEntry> entries) {
        final List<OPanelWhitelistEntry> oldEntries = getEntries();
        for(OPanelWhitelistEntry entry : oldEntries) {
            remove(entry);
        }
        for(OPanelWhitelistEntry entry : entries) {
            add(entry);
        }
    }

    @Override
    public void add(OPanelWhitelistEntry entry) {
        if(getNames().contains(entry.name)) return;
        server.getOfflinePlayer(entry.name).setWhitelisted(true);
        server.reloadWhitelist();
    }

    @Override
    public void remove(OPanelWhitelistEntry entry) {
        if(!getNames().contains(entry.name)) return;
        server.getOfflinePlayer(entry.name).setWhitelisted(false);
        server.reloadWhitelist();
    }
}
