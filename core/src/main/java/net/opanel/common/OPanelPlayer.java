package net.opanel.common;

import net.opanel.utils.Utils;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

public interface OPanelPlayer {
    String getName();
    String getUUID();
    boolean isOnline();
    OPanelInventory getInventory();
    boolean isOp();
    boolean isBanned();
    OPanelGameMode getGameMode();
    double getX();
    double getY();
    double getZ();
    void setGameMode(OPanelGameMode gamemode);
    void giveOp();
    void depriveOp();
    void kick(String reason);
    void ban(String reason);
    String getBanReason();
    void pardon();
    int getPing();
    InetAddress getAddress();

    default OPanelPlayerPosition getPosition() {
        return new OPanelPlayerPosition(getX(), getY(), getZ(), null);
    }

    default HashMap<String, Object> serialize(boolean isWhitelistEnabled, List<String> whitelistedNames, Long joinTime) {
        HashMap<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("name", getName());
        playerInfo.put("uuid", getUUID());
        playerInfo.put("isOnline", isOnline());
        playerInfo.put("isOp", isOp());
        playerInfo.put("isBanned", isBanned());
        OPanelGameMode gamemode = getGameMode();
        if(gamemode != null) playerInfo.put("gamemode", gamemode.getName());

        playerInfo.put("position", getPosition().serialize());

        final String banReason = getBanReason();
        if(banReason != null) playerInfo.put("banReason", Utils.stringToBase64(banReason));
        if(isWhitelistEnabled) playerInfo.put("isWhitelisted", whitelistedNames.contains(getName()));
        if(isOnline()) {
            playerInfo.put("ping", getPing());
            InetAddress address = getAddress();
            if(address != null) playerInfo.put("ip", address.getHostAddress());
            playerInfo.put("joinTime", joinTime);
        }
        return playerInfo;
    }
}
