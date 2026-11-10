package net.opanel.event;

import net.opanel.common.OPanelPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPositionTracker {
    private final Map<String, Map<String, Object>> positions = new ConcurrentHashMap<>();

    public void update(OPanelPlayer player) {
        Map<String, Object> position = player.serializePosition();
        if(position.equals(positions.put(player.getUUID(), position))) return;

        EventManager.get().emit(EventType.PLAYER_POSITION_UPDATE, new OPanelPlayerMoveEvent(player, position));
    }

    public void remove(String uuid) {
        positions.remove(uuid);
    }
}
