package net.opanel.event;

import net.opanel.common.OPanelPlayer;

public class OPanelPlayerMoveEvent extends OPanelEvent {
    private final OPanelPlayer player;

    public OPanelPlayerMoveEvent(OPanelPlayer player) {
        this.player = player;
    }

    public OPanelPlayer getPlayer() {
        return player;
    }
}
