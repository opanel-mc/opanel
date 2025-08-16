package net.opanel.bukkit_1_21_5;

import net.opanel.common.OPanelWhitelist;

import java.io.IOException;
import java.util.List;

/** @todo */
public class BukkitWhitelist implements OPanelWhitelist {
    @Override
    public List<String> getNames() throws IOException {
        return List.of();
    }

    @Override
    public List<OPanelWhitelistEntry> getEntries() throws IOException {
        return List.of();
    }

    @Override
    public void write(List<OPanelWhitelistEntry> entries) throws IOException {

    }

    @Override
    public void add(OPanelWhitelistEntry entry) throws IOException {

    }

    @Override
    public void remove(OPanelWhitelistEntry entry) throws IOException {

    }
}
