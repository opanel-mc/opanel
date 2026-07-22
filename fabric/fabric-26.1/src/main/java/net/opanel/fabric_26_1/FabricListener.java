package net.opanel.fabric_26_1;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.fabric_helper_unmapped.BaseFabricListener;
import net.opanel.fabric_helper_unmapped.event.PlayerGameModeChangeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FabricListener extends BaseFabricListener {
    private final Set<UUID> playersInPortal = new HashSet<>();

    public FabricListener() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::registerListeners);
    }

    private void registerListeners(MinecraftServer server) {
        ServerPlayerEvents.JOIN.register(player -> {
            EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FabricPlayer(player, server)));
        });

        ServerPlayerEvents.LEAVE.register(player -> {
            removePlayerPosition(player.getUUID());
            playersInPortal.remove(player.getUUID());
            EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new FabricPlayer(player, server)));
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
            EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new FabricPlayer(player, server), opanelGamemode));
        }));

        ServerTickEvents.END_SERVER_TICK.register(tickServer -> {
            for(var player : tickServer.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                boolean hasMoved = hasPlayerMoved(uuid, x, y, z);

                if(player.portalProcess != null) playersInPortal.add(uuid);
                else playersInPortal.remove(uuid);

                if(!hasMoved) continue;

                EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(new FabricPlayer(player, tickServer)));
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, generated) -> {
            if(!generated) return;
            if(world.dimension() != Level.OVERWORLD) return;

            ChunkPos pos = chunk.getPos();
            EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.x(), pos.z()));
        });
    }

}
