package net.opanel.neoforge_helper;

import net.minecraft.server.level.ServerPlayer;

public abstract class BaseNeoListener {
    protected final net.opanel.event.PlayerPositionTracker mapPositions = new net.opanel.event.PlayerPositionTracker();
    protected static boolean hasPlayerMoved(ServerPlayer player) {
        return player.getX() != player.xo || player.getY() != player.yo || player.getZ() != player.zo;
    }
}
