package net.opanel.neoforge_1_21_5;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.*;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.neoforge_helper.BaseNeoOfflinePlayer;

import java.io.IOException;
import java.util.UUID;

public class NeoOfflinePlayer extends BaseNeoOfflinePlayer implements OPanelPlayer {
    public NeoOfflinePlayer(MinecraftServer server, UUID uuid) {
        super(server, uuid);
    }

    @Override
    public NeoOfflineInventory getInventory() {
        return new NeoOfflineInventory(playerDataPath);
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
}
