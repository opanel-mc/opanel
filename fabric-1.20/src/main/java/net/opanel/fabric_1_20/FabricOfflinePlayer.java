package net.opanel.fabric_1_20;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.*;
import net.minecraft.util.UserCache;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.fabric_helper.BaseFabricOfflinePlayer;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class FabricOfflinePlayer extends BaseFabricOfflinePlayer implements OPanelPlayer {
    private final GameProfile profile;

    public FabricOfflinePlayer(MinecraftServer server, UUID uuid) {
        super(server, uuid);

        UserCache userCache = server.getUserCache();

        if(userCache == null) {
            throw new NullPointerException("Cannot get server user cache.");
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
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath.toFile());
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
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath.toFile());
            nbt.putInt("playerGameType", gamemode.getId());
            NbtIo.writeCompressed(nbt, playerDataPath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
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
    public void ban(String reason) {
        if(isBanned()) return;
        BannedPlayerList bannedList = playerManager.getUserBanList();
        BannedPlayerEntry entry = new BannedPlayerEntry(profile, new Date(), null, null, reason);
        bannedList.add(entry);
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        BannedPlayerEntry banEntry = playerManager.getUserBanList().get(profile);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        playerManager.getUserBanList().remove(profile);
    }
}
