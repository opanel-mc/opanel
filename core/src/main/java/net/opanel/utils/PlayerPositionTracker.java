package net.opanel.utils;

import net.opanel.common.OPanelPlayerPosition;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPositionTracker {
    private final ConcurrentHashMap<UUID, OPanelPlayerPosition> positions = new ConcurrentHashMap<>();

    public void set(UUID uuid, OPanelPlayerPosition position) {
        if(uuid == null || position == null) return;
        positions.put(uuid, position);
    }

    public OPanelPlayerPosition track(UUID uuid, double x, double y, double z, String save) {
        if(uuid == null) return null;

        OPanelPlayerPosition previous = positions.get(uuid);
        if(previous != null
                && Double.compare(previous.x(), x) == 0
                && Double.compare(previous.y(), y) == 0
                && Double.compare(previous.z(), z) == 0
                && Objects.equals(previous.save(), save)) {
            return null;
        }

        OPanelPlayerPosition current = new OPanelPlayerPosition(x, y, z, save);
        positions.put(uuid, current);
        return previous != null ? current : null;
    }

    public void remove(UUID uuid) {
        if(uuid == null) return;
        positions.remove(uuid);
    }

    public void clear() {
        positions.clear();
    }
}
