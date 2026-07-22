package net.opanel.fabric_1_20;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.fabric_helper.BaseFabricListener;
import net.opanel.fabric_helper.event.PlayerGameModeChangeEvent;

import java.util.UUID;

public class FabricListener extends BaseFabricListener {

    public FabricListener() {
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> {
            EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FabricPlayer(networkHandler.getPlayer())));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) -> {
            removePlayerPosition(networkHandler.getPlayer().getUuid());
            EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new FabricPlayer(networkHandler.getPlayer())));
        });

        PlayerGameModeChangeEvent.EVENT.register(((player, gamemode) -> {
            OPanelGameMode opanelGamemode;
            switch(gamemode) {
                case ADVENTURE -> opanelGamemode = OPanelGameMode.ADVENTURE;
                case SURVIVAL -> opanelGamemode = OPanelGameMode.SURVIVAL;
                case CREATIVE -> opanelGamemode = OPanelGameMode.CREATIVE;
                case SPECTATOR -> opanelGamemode = OPanelGameMode.SPECTATOR;
                default -> opanelGamemode = null;
            }
            EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new FabricPlayer(player), opanelGamemode));
        }));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for(var player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                if(!hasPlayerMoved(uuid, x, y, z)) continue;

                EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(new FabricPlayer(player)));
            }
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if(!hasPlayerMoved(player.getUuid(), player.getX(), player.getY(), player.getZ())) return;

            EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(new FabricPlayer(player)));
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if(world.getRegistryKey() != World.OVERWORLD) return;

            ChunkPos pos = chunk.getPos();
            EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.x, pos.z));
        });
    }

}
