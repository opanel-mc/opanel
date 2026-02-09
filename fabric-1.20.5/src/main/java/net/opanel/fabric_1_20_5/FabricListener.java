package net.opanel.fabric_1_20_5;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.fabric_helper.event.PlayerGameModeChangeEvent;

public class FabricListener {
    public FabricListener() {
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> {
            EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FabricPlayer(networkHandler.getPlayer())));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) -> {
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
    }
}
