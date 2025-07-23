package net.opanel.common;

import java.io.IOException;
import java.util.*;

public interface OPanelServer {
    byte[] getFavicon();
    String getMotd();
    String getVersion();
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
    String getPropertiesContent() throws IOException;
    void setPropertiesContent(String newContent) throws IOException;
}
