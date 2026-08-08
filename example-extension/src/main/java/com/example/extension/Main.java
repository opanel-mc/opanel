package com.example.extension;

import net.opanel.api.*;

@Extension
public class Main {
    private OPanelAPI api;

    @ExtensionLoad
    public void load(OPanelAPI api) {
        this.api = api;
        api.logInfo("Example extension loaded");

        ServerAPI server = api.getServer();
        api.logInfo("Server Info: "+ server.getServerType().getName() +" "+ server.getMinecraftVersion());
        api.logInfo("Server Motd: "+ server.getMotd());
        api.logInfo("Players: "+ server.getOnlinePlayers().size() +" / "+ server.getMaxPlayerCount());
    }

    @ExtensionUnload
    public void unload() {
        api.logInfo("Example extension unloaded");
    }
}
