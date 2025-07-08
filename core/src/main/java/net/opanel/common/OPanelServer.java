package net.opanel.common;

import java.util.List;

public interface OPanelServer {
    public String getFavicon();
    public String getMotd();
    public String getIP();
    public int getPort();
    public List<OPanelPlayer> getPlayers();
    public OPanelPlayer getPlayer(String name);
}
