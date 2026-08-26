package net.opanel.common;

import java.io.IOException;
import java.util.List;

public interface OPanelWhitelist {
    List<String> getNames() throws IOException;
    List<OPanelWhitelistEntry> getEntries() throws IOException;
    void write(List<OPanelWhitelistEntry> entries) throws IOException;
    void add(OPanelWhitelistEntry entry) throws IOException;
    void remove(OPanelWhitelistEntry entry) throws IOException;

    record OPanelWhitelistEntry(String name, String uuid) {}
}
