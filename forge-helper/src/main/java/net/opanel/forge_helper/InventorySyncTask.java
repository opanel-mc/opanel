package net.opanel.forge_helper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelPlayerInventoryChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Main-thread scheduled task for periodically syncing player inventories in Forge.
 * Acts as a safety net to catch any inventory changes missed by events.
 * 
 * THREAD SAFETY: This task MUST run on the main server thread.
 * Unlike Bukkit, Forge's TickEvent.ServerTickEvent is already on the main thread,
 * so no additional synchronization is needed. However, accessing player data
 * from async threads is still unsafe and should be avoided.
 * 
 * This class supports player slicing to reduce main thread load:
 * Instead of checking all players every tick, it checks a subset (slice)
 * of players each tick, spreading the load across multiple ticks.
 */
public class InventorySyncTask {
    private final Function<ServerPlayer, OPanelPlayer> playerFactory;
    private final ConcurrentHashMap<String, String> inventoryHashes = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private final int syncIntervalTicks;
    
    // Slicing configuration
    private static final int DEFAULT_PLAYERS_PER_TICK = 10;
    private int playersPerTick = DEFAULT_PLAYERS_PER_TICK;
    private int currentSliceIndex = 0;

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
     * Set the number of players to process per tick.
     * Lower values reduce main thread load but increase sync latency.
     * 
     * @param count Number of players to process per tick (default: 10)
     */
    public void setPlayersPerTick(int count) {
        this.playersPerTick = Math.max(1, count);
    }

    /**
     * Called every server tick from TickEvent.ServerTickEvent.
     * MUST be called from the main server thread.
     * Syncs inventories when interval is reached using player slicing.
     */
    public void onTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < syncIntervalTicks) {
            return;
        }
        tickCounter = 0;

        // Get player list snapshot
        List<ServerPlayer> allPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        int totalPlayers = allPlayers.size();
        
        if (totalPlayers == 0) {
            currentSliceIndex = 0;
            return;
        }

        // Reset slice index if it exceeds player count
        if (currentSliceIndex >= totalPlayers) {
            currentSliceIndex = 0;
        }

        // Calculate slice bounds
        int startIndex = currentSliceIndex;
        int endIndex = Math.min(startIndex + playersPerTick, totalPlayers);
        
        // Process this slice of players
        for (int i = startIndex; i < endIndex; i++) {
            ServerPlayer player = allPlayers.get(i);
            try {
                syncPlayerIfChanged(player);
            } catch (Exception e) {
                // Silently ignore errors for individual players
            }
        }

        // Move to next slice
        currentSliceIndex = endIndex;
        if (currentSliceIndex >= totalPlayers) {
            currentSliceIndex = 0;
        }
    }

    /**
     * Sync a player's inventory if it has changed.
     */
    private void syncPlayerIfChanged(ServerPlayer player) {
        if (player == null) return;
        
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
     * MUST be called from the main server thread.
     */
    public void syncPlayer(ServerPlayer player) {
        if (player == null) return;
        
        String uuid = player.getStringUUID();
        String currentHash = InventorySerializer.generateInventoryHash(player);
        inventoryHashes.put(uuid, currentHash);
        emitInventoryChange(player, uuid);
    }
}
