package net.opanel.bukkit_helper;

import com.google.gson.Gson;
import net.opanel.common.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Bukkit-specific wrapper for DatabaseManager.
 * Provides plugin-aware initialization and integration with Bukkit lifecycle.
 */
public class BukkitDatabaseManager {
    
    private static final Gson GSON = new Gson();
    private static final String DB_FILENAME = "inventory.db";
    
    private final DatabaseManager databaseManager;
    private final JavaPlugin plugin;
    private boolean initialized = false;
    
    public BukkitDatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = new DatabaseManager();
    }
    
    /**
     * Initialize the database.
     * Should be called during plugin onEnable().
     * 
     * @return true if initialization succeeded
     */
    public boolean init() {
        if (initialized) return true;
        
        try {
            // Ensure data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            Path dbPath = plugin.getDataFolder().toPath().resolve(DB_FILENAME);
            databaseManager.init(dbPath);
            initialized = true;
            
            plugin.getLogger().info("Inventory database initialized at: " + dbPath);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize inventory database: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Save player inventory asynchronously.
     * 
     * @param player The Bukkit player
     * @param inventoryData Serialized inventory map from InventorySerializer
     */
    public CompletableFuture<Void> savePlayerInventoryAsync(Player player, Map<String, Object> inventoryData) {
        if (!initialized) return CompletableFuture.completedFuture(null);
        
        String json = GSON.toJson(inventoryData);
        return databaseManager.savePlayerInventoryAsync(
            player.getUniqueId(),
            player.getName(),
            json
        );
    }
    
    /**
     * Save player inventory synchronously.
     * Use this for server shutdown scenarios.
     * 
     * @param player The Bukkit player
     * @param inventoryData Serialized inventory map from InventorySerializer
     */
    public void savePlayerInventorySync(Player player, Map<String, Object> inventoryData) {
        if (!initialized) return;
        
        String json = GSON.toJson(inventoryData);
        databaseManager.savePlayerInventorySync(
            player.getUniqueId(),
            player.getName(),
            json
        );
    }
    
    /**
     * Save player inventory with pre-serialized JSON.
     * 
     * @param uuid Player UUID
     * @param playerName Player name
     * @param inventoryJson Pre-serialized JSON string
     */
    public void savePlayerInventorySync(UUID uuid, String playerName, String inventoryJson) {
        if (!initialized) return;
        databaseManager.savePlayerInventorySync(uuid, playerName, inventoryJson);
    }
    
    /**
     * Get offline player inventory.
     * 
     * @param uuid Player UUID
     * @return CompletableFuture with PlayerData, or null if not found
     */
    public CompletableFuture<DatabaseManager.PlayerData> getOfflineInventory(UUID uuid) {
        if (!initialized) return CompletableFuture.completedFuture(null);
        return databaseManager.getOfflineInventory(uuid);
    }
    
    /**
     * Close the database connection.
     * Should be called during plugin onDisable().
     */
    public void close() {
        if (initialized) {
            databaseManager.close();
            initialized = false;
        }
    }
    
    /**
     * Check if database is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get the underlying DatabaseManager for advanced operations.
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
