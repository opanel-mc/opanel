package net.opanel.folia_1_21;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.opanel.bukkit_helper.InventorySerializer;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelPlayerInventoryChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Folia-specific inventory sync task using GlobalRegionScheduler.
 * Unlike BukkitRunnable, this uses Folia's threaded regions scheduler.
 */
public class FoliaInventorySyncTask {
    private final JavaPlugin plugin;
    private final Function<Player, OPanelPlayer> playerFactory;
    private final ConcurrentHashMap<String, String> inventoryHashes = new ConcurrentHashMap<>();
    
    private static final int DEFAULT_PLAYERS_PER_TICK = 10;
    private int playersPerTick = DEFAULT_PLAYERS_PER_TICK;
    private int currentSliceIndex = 0;
    private ScheduledTask scheduledTask;

    public FoliaInventorySyncTask(JavaPlugin plugin, Function<Player, OPanelPlayer> playerFactory) {
        this.plugin = plugin;
        this.playerFactory = playerFactory;
    }

    public void setPlayersPerTick(int count) {
        this.playersPerTick = Math.max(1, count);
    }

    /**
     * Start this sync task using Folia's GlobalRegionScheduler.
     * @param intervalTicks Interval between syncs in ticks
     * @return The scheduled task
     */
    public ScheduledTask start(long intervalTicks) {
        scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            plugin, 
            task -> run(), 
            intervalTicks, 
            intervalTicks
        );
        return scheduledTask;
    }

    public void cancel() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel();
        }
    }

    public boolean isCancelled() {
        return scheduledTask == null || scheduledTask.isCancelled();
    }

    private void run() {
        List<? extends Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
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
            Player player = allPlayers.get(i);
            try {
                processPlayer(player);
            } catch (Exception e) {
                // Silently ignore errors
            }
        }

        currentSliceIndex = endIndex;
        if (currentSliceIndex >= totalPlayers) {
            currentSliceIndex = 0;
        }
    }

    private void processPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        
        String uuid = player.getUniqueId().toString();
        String currentHash = InventorySerializer.generateInventoryHash(player);
        String previousHash = inventoryHashes.get(uuid);

        if (previousHash == null || !previousHash.equals(currentHash)) {
            inventoryHashes.put(uuid, currentHash);
            Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
            OPanelPlayer opanelPlayer = playerFactory.apply(player);
            EventManager.get().emit(
                EventType.PLAYER_INVENTORY_CHANGE,
                new OPanelPlayerInventoryChangeEvent(opanelPlayer, inventoryData)
            );
        }
    }

    public void updateHash(String uuid, String hash) {
        inventoryHashes.put(uuid, hash);
    }

    public void removePlayer(String uuid) {
        inventoryHashes.remove(uuid);
    }

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

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
