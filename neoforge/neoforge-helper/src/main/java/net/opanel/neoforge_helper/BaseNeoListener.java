package net.opanel.neoforge_helper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelPlayerPosition;
import net.opanel.utils.PlayerPositionTracker;

import java.util.UUID;

public abstract class BaseNeoListener {
    private final PlayerPositionTracker positionTracker = new PlayerPositionTracker();
    private String cachedSaveName;

    protected String getOverworldSaveName(MinecraftServer server) {
        if(cachedSaveName == null) {
            cachedSaveName = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
        }
        return cachedSaveName;
    }

    protected void seedPlayerPosition(UUID uuid, OPanelPlayerPosition position) {
        positionTracker.set(uuid, position);
    }

    protected OPanelPlayerPosition trackPlayerPosition(UUID uuid, double x, double y, double z, String save) {
        return positionTracker.track(uuid, x, y, z, save);
    }

    protected void removePlayerPosition(UUID uuid) {
        positionTracker.remove(uuid);
    }
}
