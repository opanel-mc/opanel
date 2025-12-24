package net.opanel.forge_helper;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.opanel.common.OPanelPlayer;

public abstract class BaseForgePlayer implements OPanelPlayer {
    protected final ServerPlayer player;
    protected final PlayerList playerManager;
    protected final GameProfile profile;

    public BaseForgePlayer(ServerPlayer player, MinecraftServer server) {
        this.player = player;
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
    public boolean isBanned() {
        return false;
    }

    @Override
    public String getBanReason() { return null; }

    @Override
    public void pardon() { }
}
