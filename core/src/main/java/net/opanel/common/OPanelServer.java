package net.opanel.common;

import net.opanel.ServerType;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public interface OPanelServer {
    Path serverPropertiesPath = Paths.get("").resolve("server.properties");
    Path serverIconPath = Paths.get("").resolve("server-icon.png");

    ServerType getServerType();

    default byte[] getFavicon() {
        if(!Files.exists(serverIconPath)) return null;
        try {
            return Files.readAllBytes(serverIconPath);
        } catch (IOException e) {
            return null;
        }
    }

    default void setFavicon(byte[] iconBytes) throws IOException {
        Files.write(serverIconPath, iconBytes);
    }

    String getMotd();
    void setMotd(String motd) throws IOException;
    String getVersion();
    int getPort();
    List<OPanelSave> getSaves();
    OPanelSave getSave(String saveName);
    void saveAll();
    List<OPanelPlayer> getOnlinePlayers();
    List<OPanelPlayer> getPlayers();
    int getMaxPlayerCount();
    OPanelPlayer getPlayer(String uuid);
    void removePlayerData(String uuid) throws IOException;
    List<String> getBannedIps();
    void banIp(String ip) throws UnknownHostException;
    void pardonIp(String ip) throws UnknownHostException;
    boolean isWhitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    OPanelWhitelist getWhitelist();
    void sendServerCommand(String command);
    List<String> getCommands();
    HashMap<String, Object> getGamerules();
    void setGamerules(HashMap<String, Object> gamerules);
    void reload();
    void stop();
    long getIngameTime();

    static String getPropertiesContent() throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        return Utils.readTextFile(serverPropertiesPath);
    }

    static void writePropertiesContent(String newContent) throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        Utils.writeTextFile(serverPropertiesPath, newContent);
    }
}
