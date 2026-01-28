package net.opanel.neoforge_1_21_1;

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
 * Main-thread scheduled task for periodically syncing player inventories in NeoForge.
 * 
 * THREAD SAFETY: This task MUST run on the main server thread.
 * NeoForge's ServerTickEvent.Post is already on the main thread.
 */
public class NeoInventorySyncTask {
    private final Function<ServerPlayer, OPanelPlayer> playerFactory;
    private final ConcurrentHashMap<String, String> inventoryHashes = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private final int syncIntervalTicks;
    
    private static final int DEFAULT_PLAYERS_PER_TICK = 10;
    private int playersPerTick = DEFAULT_PLAYERS_PER_TICK;
    private int currentSliceIndex = 0;

    /**
     * Create a new inventory sync task.
     * 
     * @param playerFactory Function to convert ServerPlayer to OPanelPlayer
     * @param syncIntervalTicks Interval between syncs in ticks (20 = 1 second)
     */
    public NeoInventorySyncTask(Function<ServerPlayer, OPanelPlayer> playerFactory, int syncIntervalTicks) {
        this.playerFactory = playerFactory;
        this.syncIntervalTicks = syncIntervalTicks;
    }

    /**
     * Set the number of players to process per tick.
     */
    public void setPlayersPerTick(int count) {
        this.playersPerTick = Math.max(1, count);
    }

    /**
     * Called every server tick from ServerTickEvent.Post.
     * MUST be called from the main server thread.
     */
    public void onTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < syncIntervalTicks) {
            return;
        }
        tickCounter = 0;

        List<ServerPlayer> allPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        int totalPlayers = allPlayers.size();
        
        if (totalPlayers == 0) {
            currentSliceIndex = 0;
            return;
        }

        if (currentSliceIndex >= totalPlayers) {
            currentSliceIndex = 0;
        }

        int startIndex = currentSliceIndex;
        int endIndex = Math.min(startIndex + playersPerTick, totalPlayers);
        
        for (int i = startIndex; i < endIndex; i++) {
            ServerPlayer player = allPlayers.get(i);
            try {
                syncPlayerIfChanged(player);
            } catch (Exception e) {
                // Silently ignore errors
            }
        }

        currentSliceIndex = endIndex;
        if (currentSliceIndex >= totalPlayers) {
            currentSliceIndex = 0;
        }
    }

    private void syncPlayerIfChanged(ServerPlayer player) {
        if (player == null) return;
        
        String uuid = player.getStringUUID();
        String currentHash = NeoInventorySerializer.generateInventoryHash(player);
        String previousHash = inventoryHashes.get(uuid);

        if (previousHash == null || !previousHash.equals(currentHash)) {
            inventoryHashes.put(uuid, currentHash);
            emitInventoryChange(player, uuid);
        }
    }

    private void emitInventoryChange(ServerPlayer player, String uuid) {
        Map<String, Object> inventoryData = NeoInventorySerializer.serializeInventory(player);
        OPanelPlayer opanelPlayer = playerFactory.apply(player);
        EventManager.get().emit(
            EventType.PLAYER_INVENTORY_CHANGE,
            new OPanelPlayerInventoryChangeEvent(opanelPlayer, inventoryData)
        );
    }

    /**
     * Update the hash for a specific player.
     */
    public void updateHash(String uuid, String hash) {
        inventoryHashes.put(uuid, hash);
    }

    /**
     * Remove a player from tracking.
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
        String currentHash = NeoInventorySerializer.generateInventoryHash(player);
        inventoryHashes.put(uuid, currentHash);
        emitInventoryChange(player, uuid);
    }
}
