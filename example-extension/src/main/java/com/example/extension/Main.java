package com.example.extension;

import io.javalin.http.HandlerType;
import net.opanel.api.*;
import net.opanel.api.event.PlayerInventoryChangeEvent;
import net.opanel.api.event.PlayerJoinEvent;
import net.opanel.api.server.ServerAPI;

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

        api.addHandler("/test", HandlerType.GET, ctx -> {
            ctx.result("Hello World");
        });
    }

    @ExtensionUnload
    public void unload() {
        api.logInfo("Example extension unloaded");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        api.logInfo("Player joined: "+ event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerInventoryChange(PlayerInventoryChangeEvent event) {
        api.logInfo("Player inventory changed: "+ event.getPlayer().getName());
    }
}
