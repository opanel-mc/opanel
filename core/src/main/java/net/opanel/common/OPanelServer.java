package net.opanel.common;

import java.util.List;

public interface OPanelServer {
    public String getMotd();
    public int getPort();
    public List<OPanelPlayer> getPlayers();
    public OPanelPlayer getPlayer(String name);
}
