package net.opanel.fabric_helper_unmapped;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelPlayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public abstract class BaseFabricPlayer implements OPanelPlayer {
    protected final ServerPlayer player;
    protected final MinecraftServer server;
    protected final PlayerList playerManager;
    protected final GameProfile profile;

    public BaseFabricPlayer(ServerPlayer player, MinecraftServer server) {
        this.player = player;
        this.server = server;
        playerManager = server.getPlayerList();
        profile = player.getGameProfile();
    }

    @Override
    public Map<String, Object> serializePosition() {
        String save = server.overworld().getEntity(player.getUUID()) == player
                ? server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString()
                : null;
        return OPanelPlayer.serializePosition(getX(), getY(), getZ(), save);
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
    public double getX() {
        return player.getX();
    }

    @Override
    public double getY() {
        return player.getY();
    }

    @Override
    public double getZ() {
        return player.getZ();
    }

    @Override
    public boolean isBanned() {
        return false;
    }

    @Override
    public String getBanReason() { return null; }

    @Override
    public void pardon() { }

    @Override
    public InetAddress getAddress() {
        try {
            return InetAddress.getByName(player.getIpAddress());
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
