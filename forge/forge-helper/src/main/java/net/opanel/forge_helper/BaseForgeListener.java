package net.opanel.forge_helper;

import net.minecraft.server.level.ServerPlayer;

public abstract class BaseForgeListener {
    protected final net.opanel.event.PlayerPositionTracker mapPositions = new net.opanel.event.PlayerPositionTracker();
    protected static boolean hasPlayerMoved(ServerPlayer player) {
        return player.getX() != player.xo || player.getY() != player.yo || player.getZ() != player.zo;
    }
}
