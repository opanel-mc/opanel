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

    default HashMap<String, Object> serialize(boolean isWhitelistEnabled, List<String> whitelistedNames, Long joinTime) {
        HashMap<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("name", getName());
        playerInfo.put("uuid", getUUID());
        playerInfo.put("isOnline", isOnline());
        playerInfo.put("isOp", isOp());
        playerInfo.put("isBanned", isBanned());
        playerInfo.put("gamemode", getGameMode().getName());

        HashMap<String, Double> position = new HashMap<>();
        position.put("x", getX());
        position.put("y", getY());
        position.put("z", getZ());
        playerInfo.put("position", position);

        final String banReason = getBanReason();
        if(banReason != null) playerInfo.put("banReason", Utils.stringToBase64(banReason));
        if(isWhitelistEnabled) playerInfo.put("isWhitelisted", whitelistedNames.contains(getName()));
        if(isOnline()) {
            playerInfo.put("ping", getPing());
            playerInfo.put("ip", getAddress().getHostAddress());
            playerInfo.put("joinTime", joinTime);
        }
        return playerInfo;
    }
}
