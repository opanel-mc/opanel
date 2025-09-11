package net.opanel.forge_1_20_1;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;

import java.util.Date;

public class ForgePlayer implements OPanelPlayer {
    private final ServerPlayer player;
    private final PlayerList playerManager;
    private final GameProfile profile;

    public ForgePlayer(ServerPlayer player) {
        this.player = player;
        playerManager = player.getServer().getPlayerList();
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
        switch(gamemode) {
            case ADVENTURE -> { return OPanelGameMode.ADVENTURE; }
            case SURVIVAL -> { return OPanelGameMode.SURVIVAL; }
            case CREATIVE -> { return OPanelGameMode.CREATIVE; }
            case SPECTATOR -> { return OPanelGameMode.SPECTATOR; }
        }
        return null;
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        if(player == null) return;

        switch(gamemode) {
            case ADVENTURE -> player.setGameMode(GameType.ADVENTURE);
            case SURVIVAL -> player.setGameMode(GameType.SURVIVAL);
            case CREATIVE -> player.setGameMode(GameType.CREATIVE);
            case SPECTATOR -> player.setGameMode(GameType.SPECTATOR);
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
        return player.latency;
    }
}
