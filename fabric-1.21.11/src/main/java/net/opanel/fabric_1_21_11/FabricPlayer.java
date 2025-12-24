package net.opanel.fabric_1_21_11;

import net.minecraft.server.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.fabric_helper.BaseFabricPlayer;

import java.util.Date;

public class FabricPlayer extends BaseFabricPlayer implements OPanelPlayer {
    public FabricPlayer(ServerPlayerEntity player, MinecraftServer server) {
        super(player, server);
    }

    @Override
    public boolean isOp() {
        if(player == null) return false;

        return playerManager.isOperator(new PlayerConfigEntry(profile));
    }

    @Override
    public OPanelGameMode getGameMode() {
        if(player == null) return null;

        GameMode gamemode = player.getGameMode();
        return OPanelGameMode.fromId(gamemode.getIndex());
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        if(player == null) return;

        player.changeGameMode(GameMode.byIndex(gamemode.getId()));
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        playerManager.addToOperators(new PlayerConfigEntry(profile));
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        playerManager.removeFromOperators(new PlayerConfigEntry(profile));
    }

    @Override
    public void kick(String reason) {
        player.networkHandler.disconnect(Text.of(reason));
    }

    @Override
    public void ban(String reason) {
        BannedPlayerList bannedList = playerManager.getUserBanList();
        BannedPlayerEntry entry = new BannedPlayerEntry(new PlayerConfigEntry(profile), new Date(), null, null, reason);
        bannedList.add(entry);
        kick(reason);
    }

    @Override
    public int getPing() {
        return player.networkHandler.getLatency();
    }
}
