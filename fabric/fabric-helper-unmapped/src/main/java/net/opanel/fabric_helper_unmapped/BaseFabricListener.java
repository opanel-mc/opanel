package net.opanel.fabric_helper_unmapped;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseFabricListener {
    private final Map<UUID, double[]> playerPositions = new HashMap<>();

    protected boolean hasPlayerMoved(UUID uuid, double x, double y, double z) {
        double[] previousPosition = playerPositions.get(uuid);
        if(previousPosition == null) {
            playerPositions.put(uuid, new double[] { x, y, z });
            return false;
        }
        if(x == previousPosition[0] && y == previousPosition[1] && z == previousPosition[2]) return false;

        previousPosition[0] = x;
        previousPosition[1] = y;
        previousPosition[2] = z;
        return true;
    }

    protected void removePlayerPosition(UUID uuid) {
        playerPositions.remove(uuid);
    }
}
