package net.opanel.fabric_helper;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelPlayer;

public abstract class BaseFabricPlayer implements OPanelPlayer {
    protected final ServerPlayerEntity player;
    protected final PlayerManager playerManager;
    protected final GameProfile profile;

    public BaseFabricPlayer(ServerPlayerEntity player, MinecraftServer server) {
        this.player = player;
        playerManager = server.getPlayerManager();
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
    public boolean isBanned() {
        return false;
    }

    @Override
    public String getBanReason() { return null; }

    @Override
    public void pardon() { }
}
