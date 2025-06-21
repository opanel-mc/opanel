package net.opanel.fabric_1_21_5;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;

import java.util.ArrayList;
import java.util.List;

public class FabricServer implements OPanelServer {
    private final MinecraftServer server;

    public FabricServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getMotd() {
        return server.getServerMotd();
    }

    @Override
    public int getPort() {
        return server.getServerPort();
    }

    @Override
    public List<OPanelPlayer> getPlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for(ServerPlayerEntity player : players) {
            list.add(new FabricPlayer(player));
        }
        return list;
    }

    @Override
    public OPanelPlayer getPlayer(String name) {
        for(OPanelPlayer player : getPlayers()) {
            if(player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }
}
