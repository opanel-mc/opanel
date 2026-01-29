package net.opanel.bukkit_helper;

import net.opanel.common.OPanelPlayer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelPlayerInventoryChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InventorySyncTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final Function<Player, OPanelPlayer> playerFactory;
    private final ConcurrentHashMap<String, String> inventoryHashes = new ConcurrentHashMap<>();
    
    // Database integration
    private BukkitDatabaseManager databaseManager;
    private final ConcurrentHashMap<String, PendingSave> pendingSaves = new ConcurrentHashMap<>();
    
    // Save timing (in ticks)
    private static final long DEFAULT_SAVE_INTERVAL_TICKS = 1200L; // 60 seconds
    private long saveIntervalTicks = DEFAULT_SAVE_INTERVAL_TICKS;
    private long ticksSinceLastSave = 0;
    
    private static final int DEFAULT_PLAYERS_PER_TICK = 10;
    private int playersPerTick = DEFAULT_PLAYERS_PER_TICK;
    private int currentSliceIndex = 0;

    /**
     * Create a new inventory sync task.
     * 
     * @param plugin The plugin instance for scheduling
     * @param playerFactory Function to convert Bukkit Player to OPanelPlayer
     *                      (version-specific, e.g., player -> new SpigotPlayer(plugin, player))
     */
    public InventorySyncTask(JavaPlugin plugin, Function<Player, OPanelPlayer> playerFactory) {
        this.plugin = plugin;
        this.playerFactory = playerFactory;
    }

    /**
     * Set the database manager for persistence.
     * If not set, inventory data will not be persisted.
     */
    public void setDatabaseManager(BukkitDatabaseManager manager) {
        this.databaseManager = manager;
    }

    /**
     * Set the interval between periodic database saves (in ticks).
     * 
     * @param ticks Number of ticks between saves (default: 1200 = 60 seconds)
     */
    public void setSaveIntervalTicks(long ticks) {
        this.saveIntervalTicks = Math.max(20, ticks); // minimum 1 second
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
     * Start this sync task on the main server thread.
     * This is the recommended method for Bukkit-based servers.
     * 
     * @param intervalTicks Interval between syncs in ticks (20 ticks = 1 second)
     * @return The scheduled BukkitTask
     */
    public BukkitTask start(long intervalTicks) {
        return this.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    /**
     * @deprecated Async inventory access is unsafe in Bukkit and will cause server crashes.
     *             Use {@link #start(long)} instead for main-thread execution.
     * @throws UnsupportedOperationException Always thrown - async not supported
     */
    @Deprecated
    public BukkitTask startAsync(long intervalTicks) {
        throw new UnsupportedOperationException(
            "Async inventory sync is not supported. " +
            "Bukkit.getOnlinePlayers() and PlayerInventory access must occur on the main thread. " +
            "Use start(long intervalTicks) instead."
        );
    }

    @Override
    public void run() {
        List<? extends Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPlayers = allPlayers.size();
        
        if (totalPlayers == 0) {
            currentSliceIndex = 0;
            ticksSinceLastSave = 0;
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
            Player player = allPlayers.get(i);
            try {
                processPlayer(player);
            } catch (Exception e) {
                // Silently ignore errors for individual players
            }
        }

        // Move to next slice
        currentSliceIndex = endIndex;
        if (currentSliceIndex >= totalPlayers) {
            currentSliceIndex = 0;
        }
        
        // Periodic database save (debounce mechanism)
        ticksSinceLastSave++;
        if (databaseManager != null && ticksSinceLastSave >= saveIntervalTicks) {
            flushPendingSavesAsync();
            ticksSinceLastSave = 0;
        }
    }

    /**
     * Process a single player's inventory for changes.
     */
    private void processPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        
        String uuid = player.getUniqueId().toString();
        String currentHash = InventorySerializer.generateInventoryHash(player);
        String previousHash = inventoryHashes.get(uuid);

        // Only emit event if inventory has changed
        if (previousHash == null || !previousHash.equals(currentHash)) {
            inventoryHashes.put(uuid, currentHash);
            
            // Serialize inventory data
            Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
            
            // Mark as pending save (debounce - don't save immediately)
            if (databaseManager != null) {
                pendingSaves.put(uuid, new PendingSave(player.getUniqueId(), player.getName(), inventoryData));
            }
            
            // Emit inventory change event
            OPanelPlayer opanelPlayer = playerFactory.apply(player);
            EventManager.get().emit(
                EventType.PLAYER_INVENTORY_CHANGE,
                new OPanelPlayerInventoryChangeEvent(opanelPlayer, inventoryData)
            );
        }
    }

    /**
     * Flush all pending saves to database asynchronously.
     * Called periodically by the sync task.
     */
    private void flushPendingSavesAsync() {
        if (databaseManager == null || pendingSaves.isEmpty()) return;
        
        // Take a snapshot and clear
        var toSave = new ConcurrentHashMap<>(pendingSaves);
        pendingSaves.clear();
        
        // Save each player asynchronously
        for (PendingSave save : toSave.values()) {
            databaseManager.savePlayerInventoryAsync(
                Bukkit.getPlayer(save.uuid), 
                save.inventoryData
            );
        }
    }

    /**
     * Save all pending changes synchronously.
     * MUST be called from onDisable() to prevent data loss on server shutdown.
     */
    public void saveAllPendingSync() {
        if (databaseManager == null) return;
        
        // Save all pending
        for (PendingSave save : pendingSaves.values()) {
            try {
                databaseManager.savePlayerInventorySync(save.uuid, save.playerName, 
                    new com.google.gson.Gson().toJson(save.inventoryData));
            } catch (Exception e) {
                // Log but don't throw
                plugin.getLogger().warning("Failed to save inventory for " + save.playerName + ": " + e.getMessage());
            }
        }
        pendingSaves.clear();
        
        // Also save any online players who may have changed since last pending
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
                databaseManager.savePlayerInventorySync(player, inventoryData);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save inventory for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Save a specific player's inventory immediately (async).
     * Called when a player leaves the server.
     */
    public void savePlayerOnLeave(Player player) {
        if (databaseManager == null || player == null) return;
        
        String uuid = player.getUniqueId().toString();
        
        // Remove from pending and save
        PendingSave pending = pendingSaves.remove(uuid);
        if (pending != null) {
            databaseManager.savePlayerInventoryAsync(player, pending.inventoryData);
        } else {
            // Save current inventory if no pending changes
            Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
            databaseManager.savePlayerInventoryAsync(player, inventoryData);
        }
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
        pendingSaves.remove(uuid);
    }

    /**
     * Force sync a specific player's inventory immediately.
     * Must be called from the main thread.
     */
    public void syncPlayer(Player player) {
        if (player == null) return;
        
        String uuid = player.getUniqueId().toString();
        String currentHash = InventorySerializer.generateInventoryHash(player);
        inventoryHashes.put(uuid, currentHash);
        
        Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
        
        // Mark as pending save
        if (databaseManager != null) {
            pendingSaves.put(uuid, new PendingSave(player.getUniqueId(), player.getName(), inventoryData));
        }
        
        OPanelPlayer opanelPlayer = playerFactory.apply(player);
        EventManager.get().emit(
            EventType.PLAYER_INVENTORY_CHANGE,
            new OPanelPlayerInventoryChangeEvent(opanelPlayer, inventoryData)
        );
    }

    /**
     * Get the plugin instance.
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * Internal class for pending save data.
     */
    private static class PendingSave {
        final UUID uuid;
        final String playerName;
        final Map<String, Object> inventoryData;
        
        PendingSave(UUID uuid, String playerName, Map<String, Object> inventoryData) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.inventoryData = inventoryData;
        }
    }
}
