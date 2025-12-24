package net.opanel.forge_1_21_9;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.forge_helper.BaseForgePlayer;

import java.util.Date;

public class ForgePlayer extends BaseForgePlayer implements OPanelPlayer {
    public ForgePlayer(ServerPlayer player, MinecraftServer server) {
        super(player, server);
    }

    @Override
    public boolean isOp() {
        if(player == null) return false;

        return playerManager.isOp(new NameAndId(profile));
    }

    @Override
    public OPanelGameMode getGameMode() {
        if(player == null) return null;

        GameType gamemode = player.gameMode();
        return OPanelGameMode.fromId(gamemode.getId());
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        if(player == null) return;
        player.setGameMode(GameType.byId(gamemode.getId()));
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
    public void kick(String reason) {
        player.connection.disconnect(Component.nullToEmpty(reason));
    }

    @Override
    public void ban(String reason) {
        UserBanList bannedList = playerManager.getBans();
        UserBanListEntry entry = new UserBanListEntry(new NameAndId(profile), new Date(), null, null, reason);
        bannedList.add(entry);
        kick(reason);
    }

    @Override
    public int getPing() {
        return player.connection.latency();
    }
}
