package net.opanel.fabric_1_21_5;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class FabricPlayer implements OPanelPlayer {
    private ServerPlayerEntity player;

    public FabricPlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    @Override
    public String getName() {
        if(player == null) return "";
        return player.getName().getString();
    }

    @Override
    public String getDisplayName() {
        if(player == null) return "";

        Text displayName = player.getDisplayName();
        if(displayName == null) {
            return null;
        }
        return displayName.getString();
    }

    @Override
    public String getUUID() {
        if(player == null) return null;
        return player.getUuidAsString();
    }

    @Override
    public InetSocketAddress getAddress() {
        if(player == null) return null;

        ServerPlayNetworkHandler networkHandler = player.networkHandler;
        if(networkHandler == null) return null;

        SocketAddress socketAddress = networkHandler.getConnectionAddress();
        if(socketAddress instanceof InetSocketAddress) {
            return (InetSocketAddress) socketAddress;
        } else {
            return null;
        }
    }

    @Override
    public boolean isOp() {
        if(player == null) return false;

        return player.hasPermissionLevel(2);
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
}
