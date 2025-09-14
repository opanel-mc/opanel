package net.opanel.folia_1_20;

import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.io.IOException;
import java.util.*;

public class FoliaWhitelist implements OPanelWhitelist {
    private final Main plugin;
    private final Server server;
    private final Set<OfflinePlayer> whitelist;

    public FoliaWhitelist(Main plugin, Server server, Set<OfflinePlayer> playerList) {
        this.plugin = plugin;
        this.server = server;
        whitelist = playerList;
    }

    @Override
    public List<String> getNames() throws IOException {
        List<String> list = new ArrayList<>();
        for(OfflinePlayer player : whitelist) {
            list.add(player.getName());
        }
        return list;
    }

    @Override
    public List<OPanelWhitelistEntry> getEntries() throws IOException {
        List<OPanelWhitelistEntry> entries = new ArrayList<>();
        for(OfflinePlayer player : whitelist) {
            entries.add(new OPanelWhitelistEntry(player.getName(), player.getUniqueId().toString()));
        }
        return entries;
    }

    @Override
    public void write(List<OPanelWhitelistEntry> entries) throws IOException {
        final List<OPanelWhitelistEntry> oldEntries = getEntries();
        for(OPanelWhitelistEntry entry : oldEntries) {
            remove(entry);
        }
        for(OPanelWhitelistEntry entry : entries) {
            add(entry);
        }
    }

    @Override
    public void add(OPanelWhitelistEntry entry) throws IOException {
        try {
            if(getNames().contains(entry.name)) return;
        } catch (IOException e) {
            throw e;
        }
        plugin.runTask(() -> {
            server.getOfflinePlayer(UUID.fromString(entry.uuid)).setWhitelisted(true);
            server.reloadWhitelist();
        });
    }

    @Override
    public void remove(OPanelWhitelistEntry entry) throws IOException {
        try {
            if(!getNames().contains(entry.name)) return;
        } catch (IOException e) {
            throw e;
        }
        plugin.runTask(() -> {
            server.getOfflinePlayer(UUID.fromString(entry.uuid)).setWhitelisted(false);
            server.reloadWhitelist();
        });
    }
}