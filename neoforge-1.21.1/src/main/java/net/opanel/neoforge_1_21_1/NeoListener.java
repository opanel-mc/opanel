package net.opanel.neoforge_1_21_1;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;

public class NeoListener {
    private NeoInventorySyncTask inventorySyncTask;

    public void setInventorySyncTask(NeoInventorySyncTask task) {
        this.inventorySyncTask = task;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new NeoPlayer(player)));
        
        // Sync inventory on join with delay (let inventory load)
        if (inventorySyncTask != null && player.getServer() != null) {
            // Schedule sync on next tick via server
            player.getServer().execute(() -> {
                inventorySyncTask.syncPlayer(player);
            });
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new NeoPlayer(player)));
        
        if (inventorySyncTask != null) {
            inventorySyncTask.removePlayer(player.getStringUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        final GameType gamemode = event.getNewGameMode();
        OPanelGameMode opanelGamemode;
        switch(gamemode) {
            case ADVENTURE -> opanelGamemode = OPanelGameMode.ADVENTURE;
            case SURVIVAL -> opanelGamemode = OPanelGameMode.SURVIVAL;
            case CREATIVE -> opanelGamemode = OPanelGameMode.CREATIVE;
            case SPECTATOR -> opanelGamemode = OPanelGameMode.SPECTATOR;
            default -> opanelGamemode = null;
        }
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new NeoPlayer((ServerPlayer) event.getEntity()), opanelGamemode));
    }
}

