package net.opanel.common;

import java.net.InetAddress;

public interface OPanelPlayer {
    String getName();
    String getUUID();
    boolean isOnline();
    OPanelInventory getInventory();
    boolean isOp();
    boolean isBanned();
    OPanelGameMode getGameMode();
    void setGameMode(OPanelGameMode gamemode);
    void giveOp();
    void depriveOp();
    void kick(String reason);
    void ban(String reason);
    String getBanReason();
    void pardon();
    int getPing();
    InetAddress getAddress();
}
