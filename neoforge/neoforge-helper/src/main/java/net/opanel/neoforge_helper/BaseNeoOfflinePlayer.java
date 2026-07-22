package net.opanel.neoforge_helper;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelPlayer;
import net.opanel.exception.MissingPlayerCacheException;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public abstract class BaseNeoOfflinePlayer implements OPanelPlayer {
    protected final PlayerList playerManager;
    protected final Path playerDataPath;
    private final GameProfile profile;
    protected final UUID uuid;
    private double[] position;

    public BaseNeoOfflinePlayer(MinecraftServer server, UUID uuid) {
        playerManager = server.getPlayerList();
        playerDataPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid +".dat");
        GameProfileCache profileCache = server.getProfileCache();
        this.uuid = uuid;

        if(!Files.exists(playerDataPath)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }

        if(profileCache == null) {
            throw new MissingPlayerCacheException("Cannot get player profile cache.");
        }

        ServerPlayer serverPlayer = playerManager.getPlayer(uuid);
        if(serverPlayer != null && !serverPlayer.hasDisconnected()) {
            throw new IllegalStateException("The provided player is online, please use ForgePlayer class instead.");
        }

        Optional<GameProfile> profileOpt = profileCache.get(uuid);
        if(profileOpt.isEmpty()) {
            throw new MissingPlayerCacheException("Cannot get the game profile of the provided player.");
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
    public boolean isOp() {
        return playerManager.isOp(profile);
    }

    @Override
    public boolean isBanned() {
        return playerManager.getBans().isBanned(profile);
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        playerManager.op(profile);
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        playerManager.deop(profile);
    }

    @Override
    public void kick(String reason) {
        throw new IllegalStateException("The player is offline.");
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        UserBanList bannedList = playerManager.getBans();
        UserBanListEntry entry = new UserBanListEntry(profile, new Date(), null, null, reason);
        bannedList.add(entry);
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        UserBanListEntry banEntry = playerManager.getBans().get(profile);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        playerManager.getBans().remove(profile);
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
