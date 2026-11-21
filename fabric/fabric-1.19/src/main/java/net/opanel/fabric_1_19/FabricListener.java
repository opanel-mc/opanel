package net.opanel.fabric_1_19;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayerPosition;
import net.opanel.event.*;
import net.opanel.fabric_helper.BaseFabricListener;
import net.opanel.fabric_helper.event.PlayerGameModeChangeEvent;

public class FabricListener extends BaseFabricListener {

    public FabricListener() {
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> {
            FabricPlayer player = new FabricPlayer(networkHandler.getPlayer());
            seedPlayerPosition(networkHandler.getPlayer().getUuid(), player.getPosition());
            EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(player));
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
            String overworldSave = getOverworldSaveName(server);
            for(var player : server.getPlayerManager().getPlayerList()) {
                String save = player.getWorld().getRegistryKey() == World.OVERWORLD ? overworldSave : null;
                OPanelPlayerPosition position = trackPlayerPosition(player.getUuid(), player.getX(), player.getY(), player.getZ(), save);
                if(position == null) continue;

                EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(new FabricPlayer(player), position));
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if(world.getRegistryKey() != World.OVERWORLD) return;

            ChunkPos pos = chunk.getPos();
            EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.x, pos.z));
        });
    }

}
