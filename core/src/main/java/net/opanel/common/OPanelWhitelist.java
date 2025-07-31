package net.opanel.common;

import java.io.IOException;
import java.util.List;

public interface OPanelWhitelist {
    List<String> getNames() throws IOException;
    List<OPanelWhitelistEntry> getEntries() throws IOException;
    void write(List<OPanelWhitelistEntry> entries) throws IOException;
    void add(OPanelWhitelistEntry entry) throws IOException;
    void remove(OPanelWhitelistEntry entry) throws IOException;

    class OPanelWhitelistEntry {
        public String name;
        public String uuid;

        public OPanelWhitelistEntry(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
}
