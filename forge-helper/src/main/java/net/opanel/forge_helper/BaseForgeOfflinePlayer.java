package net.opanel.forge_helper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public abstract class BaseForgeOfflinePlayer implements OPanelPlayer {
    protected static final long NBT_TRACKER_SIZE = 2097152; // 2 MB

    protected final PlayerList playerManager;
    protected final Path playerDataPath;
    protected final UUID uuid;

    public BaseForgeOfflinePlayer(MinecraftServer server, UUID uuid) {
        playerManager = server.getPlayerList();
        playerDataPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid +".dat");
        this.uuid = uuid;

        if(!Files.exists(playerDataPath)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }

        ServerPlayer serverPlayer = playerManager.getPlayer(uuid);
        if(serverPlayer != null && !serverPlayer.hasDisconnected()) {
            throw new IllegalStateException("The provided player is online, please use ForgePlayer class instead.");
        }
    }

    @Override
    public String getUUID() {
        return uuid.toString();
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public void kick(String reason) {
        throw new IllegalStateException("The player is offline.");
    }

    @Override
    public int getPing() {
        throw new IllegalStateException("The player is offline.");
    }
}
