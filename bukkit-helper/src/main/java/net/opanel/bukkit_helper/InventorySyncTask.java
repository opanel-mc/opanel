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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Async scheduled task for periodically syncing player inventories.
 * Acts as a safety net to catch any inventory changes missed by events.
 * 
 * This class is designed to be used in bukkit-helper and works across all
 * Bukkit-based server versions (Spigot, Paper, etc.).
 */
public class InventorySyncTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final Function<Player, OPanelPlayer> playerFactory;
    private final ConcurrentHashMap<String, String> inventoryHashes = new ConcurrentHashMap<>();

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
     * Start this sync task running asynchronously.
     * 
     * @param intervalTicks Interval between syncs in ticks (20 ticks = 1 second)
     * @return The scheduled BukkitTask
     */
    public BukkitTask startAsync(long intervalTicks) {
        return this.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                String uuid = player.getUniqueId().toString();
                String currentHash = InventorySerializer.generateInventoryHash(player);
                String previousHash = inventoryHashes.get(uuid);

                // Only emit event if inventory has changed
                if (previousHash == null || !previousHash.equals(currentHash)) {
                    inventoryHashes.put(uuid, currentHash);
                    
                    // Serialize inventory data
                    Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
                    
                    // Emit inventory change event
                    OPanelPlayer opanelPlayer = playerFactory.apply(player);
                    EventManager.get().emit(
                        EventType.PLAYER_INVENTORY_CHANGE,
                        new OPanelPlayerInventoryChangeEvent(opanelPlayer, inventoryData)
                    );
                }
            } catch (Exception e) {
                // Silently ignore errors for individual players
                e.printStackTrace();
            }
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
    }

    /**
     * Force sync a specific player's inventory immediately.
     */
    public void syncPlayer(Player player) {
        if (player == null) return;
        
        String uuid = player.getUniqueId().toString();
        String currentHash = InventorySerializer.generateInventoryHash(player);
        inventoryHashes.put(uuid, currentHash);
        
        Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
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
}
