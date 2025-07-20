package net.opanel.fabric_1_21_5;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;

import java.util.Date;

public class FabricPlayer implements OPanelPlayer {
    private final ServerPlayerEntity player;
    private final PlayerManager playerManager;
    private final GameProfile profile;

    public FabricPlayer(ServerPlayerEntity player) {
        this.player = player;
        playerManager = player.getServer().getPlayerManager();
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
        return player.getUuidAsString();
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public boolean isOp() {
        if(player == null) return false;

        return playerManager.isOperator(profile);
    }

    @Override
    public boolean isBanned() {
        return false;
    }

    @Override
    public OPanelGameMode getGameMode() {
        if(player == null) return null;

        GameMode gamemode = player.getGameMode();
        switch(gamemode) {
            case ADVENTURE -> { return OPanelGameMode.ADVENTURE; }
            case SURVIVAL -> { return OPanelGameMode.SURVIVAL; }
            case CREATIVE -> { return OPanelGameMode.CREATIVE; }
            case SPECTATOR -> { return OPanelGameMode.SPECTATOR; }
        }
        return null;
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
    public void kick(String reason) {
        player.networkHandler.disconnect(Text.of(reason));
    }

    @Override
    public void ban(String reason) {
        BannedPlayerList bannedList = playerManager.getUserBanList();
        BannedPlayerEntry entry = new BannedPlayerEntry(profile, new Date(), null, null, reason);
        bannedList.add(entry);
        kick(reason);
    }

    @Override
    public String getBanReason() { return null; }

    @Override
    public void pardon() { }
}
