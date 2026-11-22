package net.opanel.event;

import cn.opanel.api.event.PlayerMoveEvent;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelPlayerPosition;
import net.opanel.extension.api.ExtensionAPI;

public class OPanelPlayerMoveEvent extends OPanelEvent {
    private final OPanelPlayer player;
    private final OPanelPlayerPosition position;

    public OPanelPlayerMoveEvent(OPanelPlayer player) {
        this(player, null);
    }

    public OPanelPlayerMoveEvent(OPanelPlayer player, OPanelPlayerPosition position) {
        this.player = player;
        this.position = position;
    }

    public OPanelPlayerPosition getPosition() {
        return position != null ? position : player.getPosition();
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    @Override
    public PlayerMoveEvent toAPIEvent(ExtensionAPI api) {
        return new PlayerMoveEvent(api.createPlayerHandle(player.getUUID()));
    }
}
