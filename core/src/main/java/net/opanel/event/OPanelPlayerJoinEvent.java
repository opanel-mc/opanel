package net.opanel.event;

import cn.opanel.api.event.PlayerJoinEvent;
import net.opanel.common.OPanelPlayer;
import net.opanel.extension.api.ExtensionAPI;

public class OPanelPlayerJoinEvent extends OPanelEvent {
    private final OPanelPlayer player;

    public OPanelPlayerJoinEvent(OPanelPlayer player) {
        this.player = player;
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    @Override
    public PlayerJoinEvent toAPIEvent(ExtensionAPI api) {
        return new PlayerJoinEvent(api.createPlayerHandle(player.getUUID()));
    }
}
