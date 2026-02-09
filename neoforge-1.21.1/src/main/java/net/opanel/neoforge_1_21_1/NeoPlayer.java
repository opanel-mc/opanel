package net.opanel.neoforge_1_21_1;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class NeoPlayer implements OPanelPlayer {
    private final ServerPlayer player;
    private final MinecraftServer server;
    private final PlayerList playerManager;
    private final GameProfile profile;

    public NeoPlayer(ServerPlayer player) {
        this.player = player;
        server = player.getServer();
        playerManager = server.getPlayerList();
        profile = player.getGameProfile();
    }

    @Override
    public String getName() {
        if(player == null) return "";
        return player.getName().getString();
    }

    @Override
    public String getUUID() {
        if(player == null) return null;
        return player.getStringUUID();
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public NeoInventory getInventory() {
        return new NeoInventory(player, server);
    }

    @Override
    public boolean isOp() {
        if(player == null) return false;

        return playerManager.isOp(profile);
    }

    @Override
    public boolean isBanned() {
        return false;
    }

    @Override
    public OPanelGameMode getGameMode() {
        if(player == null) return null;

        GameType gamemode = player.gameMode.getGameModeForPlayer();
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
        playerManager.op(profile);
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        playerManager.deop(profile);
    }

    @Override
    public void kick(String reason) {
        player.connection.disconnect(Component.nullToEmpty(reason));
    }

    @Override
    public void ban(String reason) {
        UserBanList bannedList = playerManager.getBans();
        UserBanListEntry entry = new UserBanListEntry(profile, new Date(), null, null, reason);
        bannedList.add(entry);
        kick(reason);
    }

    @Override
    public String getBanReason() { return null; }

    @Override
    public void pardon() { }

    @Override
    public int getPing() {
        return player.connection.latency();
    }

    @Override
    public InetAddress getAddress() {
        try {
            return InetAddress.getByName(player.getIpAddress());
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
