package net.opanel.forge_1_21_9;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.*;
import net.minecraft.server.players.*;
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

        ProfileResolver profileCache = server.services().profileResolver();
        Optional<GameProfile> profileOpt = profileCache.fetchById(uuid);
        if(profileOpt.isEmpty()) {
            throw new NullPointerException("Cannot get the game profile of the provided player.");
        }

        profile = profileOpt.get();
    }

    @Override
    public String getName() {
        return profile.name();
    }

    @Override
    public boolean isOp() {
        return playerManager.isOp(new NameAndId(profile));
    }

    @Override
    public boolean isBanned() {
        return playerManager.getBans().isBanned(new NameAndId(profile));
    }

    @Override
    public OPanelGameMode getGameMode() {
        try {
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.create(NBT_TRACKER_SIZE));
            int gamemodeId = nbt.getIntOr("playerGameType", 0);
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
        playerManager.op(new NameAndId(profile));
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        playerManager.deop(new NameAndId(profile));
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        UserBanList bannedList = playerManager.getBans();
        UserBanListEntry entry = new UserBanListEntry(new NameAndId(profile), new Date(), null, null, reason);
        bannedList.add(entry);
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        UserBanListEntry banEntry = playerManager.getBans().get(new NameAndId(profile));
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        playerManager.getBans().remove(new NameAndId(profile));
    }
}
