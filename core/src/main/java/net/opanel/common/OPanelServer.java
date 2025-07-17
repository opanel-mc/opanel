package net.opanel.common;

import java.util.HashMap;
import java.util.List;

public interface OPanelServer {
    byte[] getFavicon();
    String getMotd();
    String getIP();
    int getPort();
    List<OPanelPlayer> getOnlinePlayers();
    List<OPanelPlayer> getPlayers();
    OPanelPlayer getPlayer(String name);
    void sendServerCommand(String command);
    List<String> getCommands();
    HashMap<String, Object> getGamerules();
}
