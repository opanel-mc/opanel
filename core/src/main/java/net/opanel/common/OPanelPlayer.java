package net.opanel.common;

import java.net.InetSocketAddress;

public interface OPanelPlayer {
    public String getName();
    public String getDisplayName();
    public String getUUID();
    public InetSocketAddress getAddress();
    public boolean isOp();
}
