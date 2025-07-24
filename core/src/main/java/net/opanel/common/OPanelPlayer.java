package net.opanel.common;

public interface OPanelPlayer {
    String getName();
    String getUUID();
    boolean isOnline();
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
}
