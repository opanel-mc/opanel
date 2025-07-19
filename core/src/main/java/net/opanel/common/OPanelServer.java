package net.opanel.common;

import java.util.*;

public interface OPanelServer {
    byte[] getFavicon();
    String getMotd();
    String getIP();
    int getPort();
    List<OPanelPlayer> getOnlinePlayers();
    List<OPanelPlayer> getPlayers();
    int getMaxPlayerCount();
    OPanelPlayer getPlayer(String uuid);
    void sendServerCommand(String command);
    List<String> getCommands();
    HashMap<String, Object> getGamerules();
    void setGamerules(HashMap<String, Object> gamerules);
    void reload();
    void stop();
}
