package net.opanel.event;

import cn.opanel.api.event.PlayerMoveEvent;
import net.opanel.common.OPanelPlayer;
import net.opanel.extension.api.ExtensionAPI;

import java.util.Map;

public class OPanelPlayerMoveEvent extends OPanelEvent {
    private final OPanelPlayer player;
    private final Map<String, Object> position;

    public OPanelPlayerMoveEvent(OPanelPlayer player) {
        this(player, null);
    }

    public OPanelPlayerMoveEvent(OPanelPlayer player, Map<String, Object> position) {
        this.player = player;
        this.position = position;
    }

    public Map<String, Object> getPosition() {
        return position != null ? position : player.serializePosition();
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    @Override
    public PlayerMoveEvent toAPIEvent(ExtensionAPI api) {
        return new PlayerMoveEvent(api.createPlayerHandle(player.getUUID()));
    }
}
