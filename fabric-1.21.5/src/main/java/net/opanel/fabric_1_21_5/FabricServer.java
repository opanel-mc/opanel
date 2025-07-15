package net.opanel.fabric_1_21_5;

import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FabricServer implements OPanelServer {
    private final MinecraftServer server;

    public FabricServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public byte[] getFavicon() {
        ServerMetadata metadata = server.getServerMetadata();
        if(metadata == null) return null;

        Optional<ServerMetadata.Favicon> faviconOptional = metadata.favicon();
        if(faviconOptional.isEmpty()) return null;

        ServerMetadata.Favicon favicon = faviconOptional.get();
        return favicon.iconBytes();
    }

    @Override
    public String getMotd() {
        return server.getServerMotd();
    }

    @Override
    public String getIP() {
        return server.getServerIp();
    }

    @Override
    public int getPort() {
        return server.getServerPort();
    }

    @Override
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for(ServerPlayerEntity player : players) {
            list.add(new FabricPlayer(player));
        }
        return list;
    }

    /** @todo */
    @Override
    public List<OPanelPlayer> getPlayers() {
        List<OPanelPlayer> list = new ArrayList<>();

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

    @Override
    public void sendCommand(String command) {
        CommandManager manager = server.getCommandManager();
        ServerCommandSource source = server.getCommandSource();
        manager.executeWithPrefix(source, command);
    }
}
