package net.opanel.event;

import cn.opanel.api.event.PlayerLeaveEvent;
import net.opanel.common.OPanelPlayer;
import net.opanel.extension.api.ExtensionAPI;

public class OPanelPlayerLeaveEvent extends OPanelEvent {
    private final OPanelPlayer player;

    public OPanelPlayerLeaveEvent(OPanelPlayer player) {
        this.player = player;
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    @Override
    public PlayerLeaveEvent toAPIEvent(ExtensionAPI api) {
        return new PlayerLeaveEvent(api.createPlayerHandle(player.getUUID()));
    }
}
