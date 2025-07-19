package net.opanel.common;

import java.net.InetSocketAddress;

public interface OPanelPlayer {
    String getName();
    String getUUID();
    boolean isOnline();
    boolean isOp();
    boolean isBanned();
    OPanelGameMode getGameMode();
}
