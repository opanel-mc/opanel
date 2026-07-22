package net.opanel.neoforge_helper;

import net.minecraft.server.level.ServerPlayer;

public abstract class BaseNeoListener {
    protected static boolean hasPlayerMoved(ServerPlayer player) {
        return player.getX() != player.xo || player.getY() != player.yo || player.getZ() != player.zo;
    }
}
