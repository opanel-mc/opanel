package net.opanel.bukkit_1_21_5;

import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class BukkitServer implements OPanelServer {
    private static final Path serverPropertiesPath = Paths.get("").resolve("server.properties");

    private final Server server;

    public BukkitServer(Server server) {
        this.server = server;
    }

    @Override
    public byte[] getFavicon() {
        try {
            return server.getServerIcon().getData().getBytes(StandardCharsets.UTF_8);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getMotd() {
        return "";
    }

    @Override
    public void setMotd(String motd) throws IOException {

    }

    @Override
    public String getVersion() {
        return server.getVersion();
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Override
    public List<OPanelSave> getSaves() {
        return List.of();
    }

    @Override
    public OPanelSave getSave(String saveName) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        Collection<Player> players = (Collection<Player>) server.getOnlinePlayers();
        for(Player serverPlayer : players) {
            BukkitPlayer player = new BukkitPlayer(serverPlayer);
            list.add(player);
        }
        return list;
    }

    @Override
    public List<OPanelPlayer> getPlayers() {
        List<OPanelPlayer> list = new ArrayList<>(getOnlinePlayers());
        OfflinePlayer[] players = server.getOfflinePlayers();
        for(OfflinePlayer offlinePlayer : players) {
            Player serverPlayer = offlinePlayer.getPlayer();
            if(serverPlayer == null) continue;
            BukkitPlayer player = new BukkitPlayer(serverPlayer);
            list.add(player);
        }
        return list;
    }

    @Override
    public int getMaxPlayerCount() {
        return server.getMaxPlayers();
    }

    @Override
    public OPanelPlayer getPlayer(String uuid) {
        return null;
    }

    @Override
    public boolean isWhitelistEnabled() {
        return server.hasWhitelist();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        server.setWhitelist(enabled);
    }

    @Override
    public OPanelWhitelist getWhitelist() {
        return null;
    }

    @Override
    public void sendServerCommand(String command) {

    }

    @Override
    public List<String> getCommands() {
        return List.of();
    }

    @Override
    public HashMap<String, Object> getGamerules() {
        return null;
    }

    @Override
    public void setGamerules(HashMap<String, Object> gamerules) {

    }

    @Override
    public void reload() {
        server.reload();
    }

    @Override
    public void stop() {

    }

    @Override
    public String getPropertiesContent() throws IOException {
        return "";
    }

    @Override
    public void writePropertiesContent(String newContent) throws IOException {

    }

    @Override
    public long getIngameTime() {
        return 0;
    }
}
