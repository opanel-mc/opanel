package net.opanel.forge_helper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelPlayerInventoryChangeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Periodic task for syncing player inventories in Forge.
 * Acts as a safety net to catch any inventory changes missed by events.
 * 
 * Unlike Bukkit, Forge doesn't have BukkitRunnable, so this uses
 * a simple tick counter approach via ServerTickEvent.
 */
public class InventorySyncTask {
    private final Function<ServerPlayer, OPanelPlayer> playerFactory;
    private final ConcurrentHashMap<String, String> inventoryHashes = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private final int syncIntervalTicks;

    /**
     * Create a new inventory sync task.
     * 
     * @param playerFactory Function to convert ServerPlayer to OPanelPlayer
     * @param syncIntervalTicks Interval between syncs in ticks (20 = 1 second)
     */
    public InventorySyncTask(Function<ServerPlayer, OPanelPlayer> playerFactory, int syncIntervalTicks) {
        this.playerFactory = playerFactory;
        this.syncIntervalTicks = syncIntervalTicks;
    }

    /**
     * Called every server tick. Syncs inventories when interval is reached.
     */
    public void onTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < syncIntervalTicks) {
            return;
        }
        tickCounter = 0;

        // Run sync on all online players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                syncPlayerIfChanged(player);
            } catch (Exception e) {
                // Silently ignore errors for individual players
            }
        }
    }

    /**
     * Sync a player's inventory if it has changed.
     */
    private void syncPlayerIfChanged(ServerPlayer player) {
        String uuid = player.getStringUUID();
        String currentHash = InventorySerializer.generateInventoryHash(player);
        String previousHash = inventoryHashes.get(uuid);

        if (previousHash == null || !previousHash.equals(currentHash)) {
            inventoryHashes.put(uuid, currentHash);
            emitInventoryChange(player, uuid);
        }
    }

    /**
     * Emit inventory change event for a player.
     */
    private void emitInventoryChange(ServerPlayer player, String uuid) {
        Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
        OPanelPlayer opanelPlayer = playerFactory.apply(player);
        EventManager.get().emit(
            EventType.PLAYER_INVENTORY_CHANGE,
            new OPanelPlayerInventoryChangeEvent(opanelPlayer, inventoryData)
        );
    }

    /**
     * Update the hash for a specific player.
     * Called by event handlers to mark inventory as recently synced.
     */
    public void updateHash(String uuid, String hash) {
        inventoryHashes.put(uuid, hash);
    }

    /**
     * Remove a player from tracking.
     * Called when player leaves the server.
     */
    public void removePlayer(String uuid) {
        inventoryHashes.remove(uuid);
    }

    /**
     * Force sync a specific player's inventory immediately.
     */
    public void syncPlayer(ServerPlayer player) {
        if (player == null) return;
        
        String uuid = player.getStringUUID();
        String currentHash = InventorySerializer.generateInventoryHash(player);
        inventoryHashes.put(uuid, currentHash);
        emitInventoryChange(player, uuid);
    }
}
