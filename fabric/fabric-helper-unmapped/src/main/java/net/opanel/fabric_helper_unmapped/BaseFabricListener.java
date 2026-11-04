package net.opanel.fabric_helper_unmapped;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseFabricListener {
    protected static String mapWorldName(net.minecraft.server.level.ServerLevel world) {
        var server = world.getServer();
        String name = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE).getParent().getFileName().toString();
        if(world == server.getLevel(net.minecraft.world.level.Level.NETHER)) return name + "_nether";
        if(world == server.getLevel(net.minecraft.world.level.Level.END)) return name + "_the_end";
        return world == server.overworld() ? name : null;
    }

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
