package com.example.extension;

import net.opanel.api.*;

@Extension
public class Main {
    private OPanelAPI api;

    @ExtensionLoad
    public void load(OPanelAPI api) {
        this.api = api;
        api.logInfo("Example extension loaded with OPanel " + api.getOPanelVersion());
    }

    @ExtensionUnload
    public void unload() {
        api.logInfo("Example extension unloaded");
    }
}
