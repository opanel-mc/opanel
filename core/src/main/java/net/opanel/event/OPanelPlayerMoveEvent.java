package net.opanel.event;

import cn.opanel.api.event.PlayerMoveEvent;
import net.opanel.common.OPanelPlayer;
import net.opanel.extension.api.ExtensionAPI;

public class OPanelPlayerMoveEvent extends OPanelEvent {
    private final OPanelPlayer player;

    public OPanelPlayerMoveEvent(OPanelPlayer player) {
        this.player = player;
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    @Override
    public PlayerMoveEvent toAPIEvent(ExtensionAPI api) {
        return new PlayerMoveEvent(api.createPlayerHandle(player.getUUID()));
    }
}
