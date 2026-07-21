package net.opanel.fabric_helper_unmapped;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelPlayer;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public abstract class BaseFabricOfflinePlayer implements OPanelPlayer {
    protected final PlayerList playerManager;
    protected final Path playerDataPath;
    protected final UUID uuid;
    private double[] position;

    public BaseFabricOfflinePlayer(MinecraftServer server, UUID uuid) {
        playerManager = server.getPlayerList();
        playerDataPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid +".dat");
        this.uuid = uuid;

        if(!Files.exists(playerDataPath)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }

        ServerPlayer serverPlayer = playerManager.getPlayer(uuid);
        if(serverPlayer != null && !serverPlayer.hasDisconnected()) {
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

    protected abstract double[] readPosition();

    private double[] getPosition() {
        if(position == null) position = readPosition();
        return position;
    }

    @Override
    public double getX() {
        return getPosition()[0];
    }

    @Override
    public double getY() {
        return getPosition()[1];
    }

    @Override
    public double getZ() {
        return getPosition()[2];
    }

    @Override
    public void kick(String reason) {
        throw new IllegalStateException("The player is offline.");
    }

    @Override
    public int getPing() {
        throw new IllegalStateException("The player is offline.");
    }

    @Override
    public InetAddress getAddress() {
        throw new IllegalStateException("The player is offline.");
    }
}
