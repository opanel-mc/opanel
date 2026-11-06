package net.opanel.event;

import cn.opanel.api.event.PlayerMoveEvent;
import net.opanel.common.OPanelPlayer;
import net.opanel.extension.api.ExtensionAPI;

import java.util.Map;

public class OPanelPlayerMoveEvent extends OPanelEvent {
    private final OPanelPlayer player;
    private final Map<String, Object> position;
    private final boolean extensionOnly;

    public OPanelPlayerMoveEvent(OPanelPlayer player) {
        this(player, null);
    }

    public OPanelPlayerMoveEvent(OPanelPlayer player, Map<String, Object> position) {
        this(player, position, false);
    }

    private OPanelPlayerMoveEvent(OPanelPlayer player, Map<String, Object> position, boolean extensionOnly) {
        this.player = player;
        this.position = position;
        this.extensionOnly = extensionOnly;
    }

    public static OPanelPlayerMoveEvent forExtensions(OPanelPlayer player) {
        return new OPanelPlayerMoveEvent(player, null, true);
    }

    public boolean isExtensionOnly() {
        return extensionOnly;
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
