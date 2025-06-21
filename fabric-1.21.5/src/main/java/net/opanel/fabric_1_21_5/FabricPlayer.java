package net.opanel.fabric_1_21_5;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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

        MinecraftServer server = player.getServer();
        String[] ops = server.getPlayerManager().getOpList().getNames();
        for(String name : ops) {
            if(getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
