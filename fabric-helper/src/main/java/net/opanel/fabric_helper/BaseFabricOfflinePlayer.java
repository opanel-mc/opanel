package net.opanel.fabric_helper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.opanel.common.OPanelPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public abstract class BaseFabricOfflinePlayer implements OPanelPlayer {
    protected static final long NBT_TRACKER_SIZE = 2097152; // 2 MB

    protected final PlayerManager playerManager;
    protected final Path playerDataPath;
    protected final UUID uuid;

    public BaseFabricOfflinePlayer(MinecraftServer server, UUID uuid) {
        playerManager = server.getPlayerManager();
        playerDataPath = server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuid +".dat");
        this.uuid = uuid;

        if(!Files.exists(playerDataPath)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }

        ServerPlayerEntity serverPlayer = playerManager.getPlayer(uuid);
        if(serverPlayer != null && !serverPlayer.isDisconnected()) {
            throw new IllegalStateException("The provided player is online, please use FabricPlayer class instead.");
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
