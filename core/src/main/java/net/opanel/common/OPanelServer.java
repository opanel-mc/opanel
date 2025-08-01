package net.opanel.common;

import java.io.IOException;
import java.util.*;

public interface OPanelServer {
    byte[] getFavicon();
    String getMotd();
    void setMotd(String motd) throws IOException;
    String getVersion();
    int getPort();
    List<OPanelSave> getSaves();
    OPanelSave getSave(String saveName);
    List<OPanelPlayer> getOnlinePlayers();
    List<OPanelPlayer> getPlayers();
    int getMaxPlayerCount();
    OPanelPlayer getPlayer(String uuid);
    boolean isWhitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    OPanelWhitelist getWhitelist();
    void sendServerCommand(String command);
    List<String> getCommands();
    HashMap<String, Object> getGamerules();
    void setGamerules(HashMap<String, Object> gamerules);
    void reload();
    void stop();
    String getPropertiesContent() throws IOException;
    void writePropertiesContent(String newContent) throws IOException;
}
