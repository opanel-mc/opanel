package net.opanel.paper_helper;

import net.opanel.map.MapPlayerPositions;
import org.bukkit.Bukkit;
import java.util.ArrayList;

public final class PaperMapPlayerSampler {
    private PaperMapPlayerSampler() {}
    /** Called on the synchronous Paper tick-end event. */
    public static void tick() {
        if(!MapPlayerPositions.hasDemand()) return;
        long now = System.currentTimeMillis();
        if(!MapPlayerPositions.needsSample(now)) return;
        var positions = new ArrayList<MapPlayerPositions.Position>();
        for(var player : Bukkit.getOnlinePlayers()) {
            var location = player.getLocation();
            if(location.getWorld() == null) continue;
            positions.add(new MapPlayerPositions.Position(player.getUniqueId().toString(), player.getName(),
                    location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
        }
        MapPlayerPositions.publish(now, positions);
    }
}
