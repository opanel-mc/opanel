package net.opanel.fabric_1_21_5;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import net.minecraft.world.GameMode;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class FabricOfflinePlayer implements OPanelPlayer {
    private final PlayerManager playerManager;
    private final Path playerDataPath;
    private final GameProfile profile;

    private final UUID uuid;

    public FabricOfflinePlayer(MinecraftServer server, Path playerDataPath, UUID uuid) {
        playerManager = server.getPlayerManager();
        this.playerDataPath = playerDataPath;
        UserCache userCache = server.getUserCache();
        this.uuid = uuid;

        if(userCache == null) {
            throw new NullPointerException("Cannot get server user cache.");
        }

        ServerPlayerEntity serverPlayer = playerManager.getPlayer(uuid);
        if(serverPlayer != null && !serverPlayer.isDisconnected()) {
            throw new IllegalStateException("The provided player is online, please use FabricPlayer class instead.");
        }

        Optional<GameProfile> profileOpt = userCache.getByUuid(uuid);
        if(profileOpt.isEmpty()) {
            throw new NullPointerException("Cannot get the game profile of the provided player.");
        }

        profile = profileOpt.get();
    }

    @Override
    public String getName() {
        return profile.getName();
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
    public boolean isOp() {
        return playerManager.isOperator(profile);
    }

    @Override
    public boolean isBanned() {
        return playerManager.getUserBanList().contains(profile);
    }

    @Override
    public OPanelGameMode getGameMode() {
        try {
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.of(2097152L)); // 2 MB
            int gamemodeId = nbt.getInt("playerGameType", 0);
            GameMode gamemode = GameMode.byIndex(gamemodeId);
            switch(gamemode) {
                case ADVENTURE -> { return OPanelGameMode.ADVENTURE; }
                case SURVIVAL -> { return OPanelGameMode.SURVIVAL; }
                case CREATIVE -> { return OPanelGameMode.CREATIVE; }
                case SPECTATOR -> { return OPanelGameMode.SPECTATOR; }
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        playerManager.addToOperators(profile);
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        playerManager.removeFromOperators(profile);
    }

    @Override
    public void kick(String reason) {
        throw new IllegalStateException("The player is offline.");
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        BannedPlayerList bannedList = playerManager.getUserBanList();
        BannedPlayerEntry entry = new BannedPlayerEntry(profile, new Date(), null, null, reason);
        bannedList.add(entry);
    }
}
