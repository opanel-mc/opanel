package net.opanel.fabric_1_21_9;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.*;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.fabric_helper.BaseFabricOfflinePlayer;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class FabricOfflinePlayer extends BaseFabricOfflinePlayer implements OPanelPlayer {
    private final PlayerConfigEntry configEntry;

    public FabricOfflinePlayer(MinecraftServer server, UUID uuid) {
        super(server, uuid);

        Optional<PlayerConfigEntry> configEntryOptional = server.getApiServices().nameToIdCache().getByUuid(uuid);
        if(configEntryOptional.isEmpty()) {
            throw new RuntimeException("Cannot get the cache of the provided player.");
        }
        configEntry = configEntryOptional.get();
    }

    @Override
    public String getName() {
        return configEntry.name();
    }

    @Override
    public boolean isOp() {
        return playerManager.isOperator(configEntry);
    }

    @Override
    public boolean isBanned() {
        return playerManager.getUserBanList().contains(configEntry);
    }

    @Override
    public OPanelGameMode getGameMode() {
        try {
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            int gamemodeId = nbt.getInt("playerGameType", 0);
            return OPanelGameMode.fromId(gamemodeId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        try {
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            nbt.putInt("playerGameType", gamemode.getId());
            NbtIo.writeCompressed(nbt, playerDataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        playerManager.addToOperators(configEntry);
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        playerManager.removeFromOperators(configEntry);
    }

    @Override
    public FabricOfflineInventory getInventory() {
        return new FabricOfflineInventory(playerDataPath);
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        BannedPlayerList bannedList = playerManager.getUserBanList();
        BannedPlayerEntry entry = new BannedPlayerEntry(configEntry, new Date(), null, null, reason);
        bannedList.add(entry);
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        BannedPlayerEntry banEntry = playerManager.getUserBanList().get(configEntry);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        playerManager.getUserBanList().remove(configEntry);
    }
}
