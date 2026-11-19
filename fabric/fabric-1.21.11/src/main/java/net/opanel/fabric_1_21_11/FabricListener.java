package net.opanel.fabric_1_21_11;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayerPosition;
import net.opanel.event.*;
import net.opanel.fabric_helper.BaseFabricListener;
import net.opanel.fabric_helper.event.PlayerGameModeChangeEvent;

public class FabricListener extends BaseFabricListener {

    public FabricListener() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::registerListeners);
    }

    private void registerListeners(MinecraftServer server) {
        ServerPlayerEvents.JOIN.register(player -> {
            FabricPlayer wrapped = new FabricPlayer(player, server);
            seedPlayerPosition(player.getUuid(), wrapped.getPosition());
            EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(wrapped));
        });

        ServerPlayerEvents.LEAVE.register(player -> {
            removePlayerPosition(player.getUuid());
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
            String overworldSave = getOverworldSaveName(tickServer);
            for(var player : tickServer.getPlayerManager().getPlayerList()) {
                String save = player.getEntityWorld().getRegistryKey() == World.OVERWORLD ? overworldSave : null;
                OPanelPlayerPosition position = trackPlayerPosition(player.getUuid(), player.getX(), player.getY(), player.getZ(), save);
                if(position == null) continue;

                EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(new FabricPlayer(player, tickServer), position));
            }
        });

        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if(world.getRegistryKey() != World.OVERWORLD) return;

            ChunkPos pos = chunk.getPos();
            EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.x, pos.z));
        });
    }

}
