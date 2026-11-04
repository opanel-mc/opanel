package net.opanel.forge_helper;

import net.minecraft.server.level.ServerPlayer;

public abstract class BaseForgeListener {
    protected static String mapWorldName(net.minecraft.server.level.ServerLevel world) {
        var server = world.getServer();
        String name = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE).getParent().getFileName().toString();
        if(world == server.getLevel(net.minecraft.world.level.Level.NETHER)) return name + "_nether";
        if(world == server.getLevel(net.minecraft.world.level.Level.END)) return name + "_the_end";
        return world == server.overworld() ? name : null;
    }

    protected static boolean hasPlayerMoved(ServerPlayer player) {
        return player.getX() != player.xo || player.getY() != player.yo || player.getZ() != player.zo;
    }
}
