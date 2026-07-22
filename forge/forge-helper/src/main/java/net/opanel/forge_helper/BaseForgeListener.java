package net.opanel.forge_helper;

import net.minecraft.server.level.ServerPlayer;

public abstract class BaseForgeListener {
    protected static boolean hasPlayerMoved(ServerPlayer player) {
        return player.getX() != player.xo || player.getY() != player.yo || player.getZ() != player.zo;
    }
}
