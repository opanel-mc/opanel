package net.opanel.common;

import java.net.InetSocketAddress;

public interface OPanelPlayer {
    String getName();
    String getDisplayName();
    String getUUID();
    InetSocketAddress getAddress();
    boolean isOp();
}
