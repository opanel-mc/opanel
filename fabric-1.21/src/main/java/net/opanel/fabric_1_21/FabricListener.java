package net.opanel.fabric_1_21;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.fabric_helper.InventorySerializer;
import net.opanel.fabric_helper.InventorySyncTask;
import net.opanel.fabric_helper.event.PlayerGameModeChangeEvent;

import java.util.Map;

/**
 * Fabric 1.21 event listener for player events.
 * 
 * THREAD SAFETY:
 * - ServerPlayConnectionEvents callbacks run on the main server thread
 * - PlayerGameModeChangeEvent callbacks run on the main server thread
 * - All inventory operations MUST be performed on the main thread
 * - Use server.execute() for delayed operations to ensure main-thread execution
 */
public class FabricListener {
    private InventorySyncTask inventorySyncTask;

    public FabricListener() {
        // JOIN event - runs on main thread
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> {
            ServerPlayerEntity player = networkHandler.getPlayer();
            EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FabricPlayer(player)));
            
            // Sync inventory on join - use server.execute() to ensure main-thread execution
            // Delayed by one tick to ensure inventory is fully loaded
            server.execute(() -> {
                if (inventorySyncTask != null && player != null) {
                    inventorySyncTask.syncPlayer(player);
                }
            });
        });

        // DISCONNECT event - runs on main thread
        ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) -> {
            ServerPlayerEntity player = networkHandler.getPlayer();
            EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new FabricPlayer(player)));
            
            // Remove player from inventory tracking
            if (inventorySyncTask != null && player != null) {
                inventorySyncTask.removePlayer(player.getUuidAsString());
            }
        });

        // Game mode change event - runs on main thread
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

    public void setInventorySyncTask(InventorySyncTask task) {
        this.inventorySyncTask = task;
    }

    /**
     * Emit inventory change event for a player.
     * MUST be called from the main server thread.
     * Called by inventory-related event handlers.
     */
    public void emitInventoryChange(ServerPlayerEntity player) {
        if (player == null) return;
        
        String uuid = player.getUuidAsString();
        Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
        String hash = InventorySerializer.generateInventoryHash(player);
        
        if (inventorySyncTask != null) {
            inventorySyncTask.updateHash(uuid, hash);
        }
        
        EventManager.get().emit(
            EventType.PLAYER_INVENTORY_CHANGE,
            new OPanelPlayerInventoryChangeEvent(new FabricPlayer(player), inventoryData)
        );
    }
}
