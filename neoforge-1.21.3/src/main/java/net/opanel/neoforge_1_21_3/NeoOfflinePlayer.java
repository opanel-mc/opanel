package net.opanel.neoforge_1_21_3;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class NeoOfflinePlayer implements OPanelPlayer {
    private final PlayerList playerManager;
    private final Path playerDataPath;
    private final GameProfile profile;

    private final UUID uuid;

    public NeoOfflinePlayer(MinecraftServer server, UUID uuid) {
        playerManager = server.getPlayerList();
        playerDataPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid +".dat");
        GameProfileCache profileCache = server.getProfileCache();
        this.uuid = uuid;

        if(!Files.exists(playerDataPath)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }

        if(profileCache == null) {
            throw new NullPointerException("Cannot get player profile cache.");
        }

        ServerPlayer serverPlayer = playerManager.getPlayer(uuid);
        if(serverPlayer != null && !serverPlayer.hasDisconnected()) {
            throw new IllegalStateException("The provided player is online, please use ForgePlayer class instead.");
        }

        Optional<GameProfile> profileOpt = profileCache.get(uuid);
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
        return playerManager.isOp(profile);
    }

    @Override
    public boolean isBanned() {
        return playerManager.getBans().isBanned(profile);
    }

    @Override
    public OPanelGameMode getGameMode() {
        try {
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.create(2097152L)); // 2 MB
            int gamemodeId = nbt.getInt("playerGameType");
            GameType gamemode = GameType.byId(gamemodeId);
            switch(gamemode) {
                case ADVENTURE -> { return OPanelGameMode.ADVENTURE; }
                case SURVIVAL -> { return OPanelGameMode.SURVIVAL; }
                case CREATIVE -> { return OPanelGameMode.CREATIVE; }
                case SPECTATOR -> { return OPanelGameMode.SPECTATOR; }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        try {
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.create(2097152L)); // 2 MB
            switch(gamemode) {
                case ADVENTURE -> nbt.putInt("playerGameType", 2);
                case SURVIVAL -> nbt.putInt("playerGameType", 0);
                case CREATIVE -> nbt.putInt("playerGameType", 1);
                case SPECTATOR -> nbt.putInt("playerGameType", 3);
            }
            NbtIo.writeCompressed(nbt, playerDataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
