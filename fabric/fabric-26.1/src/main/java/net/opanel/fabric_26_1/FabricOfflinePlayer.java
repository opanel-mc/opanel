package net.opanel.fabric_26_1;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.exception.MissingPlayerCacheException;
import net.opanel.fabric_helper_unmapped.BaseFabricOfflinePlayer;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class FabricOfflinePlayer extends BaseFabricOfflinePlayer implements OPanelPlayer {
    private final NameAndId nameAndId;

    public FabricOfflinePlayer(MinecraftServer server, UUID uuid) {
        super(server, uuid);

        Optional<NameAndId> nameAndIdOptional = server.services().nameToIdCache().get(uuid);
        if(nameAndIdOptional.isEmpty()) {
            throw new MissingPlayerCacheException("Cannot get the cache of the provided player.");
        }
        nameAndId = nameAndIdOptional.get();
    }

    @Override
    public String getName() {
        return nameAndId.name();
    }

    @Override
    public boolean isOp() {
        return playerManager.isOp(nameAndId);
    }

    @Override
    public boolean isBanned() {
        return playerManager.getBans().isBanned(nameAndId);
    }

    @Override
    protected double[] readPosition() {
        try {
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
            var pos = nbt.getListOrEmpty("Pos");
            return new double[]{pos.getDoubleOr(0, 0), pos.getDoubleOr(1, 0), pos.getDoubleOr(2, 0)};
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new double[3];
    }

    @Override
    public OPanelGameMode getGameMode() {
        try {
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
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
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
            nbt.putInt("playerGameType", gamemode.getId());
            NbtIo.writeCompressed(nbt, playerDataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        playerManager.op(nameAndId);
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        playerManager.deop(nameAndId);
    }

    @Override
    public FabricOfflineInventory getInventory() {
        return new FabricOfflineInventory(playerDataPath);
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        UserBanList bannedList = playerManager.getBans();
        UserBanListEntry entry = new UserBanListEntry(nameAndId, new Date(), null, null, reason);
        bannedList.add(entry);
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        UserBanListEntry banEntry = playerManager.getBans().get(nameAndId);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        playerManager.getBans().remove(nameAndId);
    }
}
