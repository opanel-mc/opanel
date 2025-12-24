package net.opanel.forge_1_21_3;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.*;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.forge_helper.BaseForgeOfflinePlayer;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class ForgeOfflinePlayer extends BaseForgeOfflinePlayer implements OPanelPlayer {
    private final GameProfile profile;

    public ForgeOfflinePlayer(MinecraftServer server, UUID uuid) {
        super(server, uuid);

        GameProfileCache profileCache = server.getProfileCache();

        if(profileCache == null) {
            throw new NullPointerException("Cannot get player profile cache.");
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
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.create(NBT_TRACKER_SIZE));
            int gamemodeId = nbt.getInt("playerGameType");
            return OPanelGameMode.fromId(gamemodeId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        try {
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.create(NBT_TRACKER_SIZE));
            nbt.putInt("playerGameType", gamemode.getId());
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
}
