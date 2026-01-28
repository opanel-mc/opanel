package net.opanel.forge_1_19_4;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.forge_helper.InventorySerializer;
import net.opanel.forge_helper.InventorySyncTask;

import java.util.Map;

/**
 * Forge 1.19.4 event listener for player events.
 */
public class ForgeListener {
    private InventorySyncTask inventorySyncTask;

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new ForgePlayer(player)));
        
        if (inventorySyncTask != null) {
            inventorySyncTask.syncPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new ForgePlayer(player)));
        
        if (inventorySyncTask != null) {
            inventorySyncTask.removePlayer(player.getStringUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        final GameType gamemode = event.getNewGameMode();
        OPanelGameMode opanelGamemode;
        switch(gamemode) {
            case ADVENTURE -> opanelGamemode = OPanelGameMode.ADVENTURE;
            case SURVIVAL -> opanelGamemode = OPanelGameMode.SURVIVAL;
            case CREATIVE -> opanelGamemode = OPanelGameMode.CREATIVE;
            case SPECTATOR -> opanelGamemode = OPanelGameMode.SPECTATOR;
            default -> opanelGamemode = null;
        }
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new ForgePlayer(player), opanelGamemode));
    }

    public void setInventorySyncTask(InventorySyncTask task) {
        this.inventorySyncTask = task;
    }

    public void emitInventoryChange(ServerPlayer player) {
        if (player == null) return;
        
        String uuid = player.getStringUUID();
        Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
        String hash = InventorySerializer.generateInventoryHash(player);
        
        if (inventorySyncTask != null) {
            inventorySyncTask.updateHash(uuid, hash);
        }
        
        EventManager.get().emit(
            EventType.PLAYER_INVENTORY_CHANGE,
            new OPanelPlayerInventoryChangeEvent(new ForgePlayer(player), inventoryData)
        );
    }
}

