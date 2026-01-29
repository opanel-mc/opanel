package net.opanel.common;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Database manager for persisting player inventory data to SQLite.
 * Uses Code-First pattern: automatically creates database and tables on init.
 * 
 * Thread Safety:
 * - All write operations run async via ExecutorService
 * - Uses single connection with synchronized access (SQLite limitation)
 * - Provides sync save method for server shutdown scenarios
 */
public class DatabaseManager {
    
    private static final String CREATE_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS player_data (" +
        "uuid VARCHAR(36) PRIMARY KEY," +
        "player_name VARCHAR(255)," +
        "inventory_json TEXT," +
        "last_updated BIGINT," +
        "is_dirty INTEGER DEFAULT 0" +
        ")";
    
    private static final String UPSERT_SQL = 
        "INSERT OR REPLACE INTO player_data " +
        "(uuid, player_name, inventory_json, last_updated, is_dirty) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String SELECT_SQL = 
        "SELECT player_name, inventory_json, last_updated, is_dirty FROM player_data WHERE uuid = ?";
    
    private Connection connection;
    private final ExecutorService executor;
    private Path dbPath;
    
    public DatabaseManager() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "OPanel-Database");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Initialize the database connection and create tables.
     * Must be called before any other operations.
     * 
     * @param dbPath Path to the SQLite database file
     * @throws SQLException if connection or table creation fails
     */
    public synchronized void init(Path dbPath) throws SQLException {
        this.dbPath = dbPath;
        
        // Ensure parent directory exists
        dbPath.getParent().toFile().mkdirs();
        
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        this.connection = DriverManager.getConnection(url);
        
        // Create table if not exists
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        }
    }
    
    /**
     * Save player inventory asynchronously.
     * Uses UPSERT semantics (INSERT OR REPLACE).
     * 
     * @param uuid Player UUID
     * @param playerName Player display name (for human readability)
     * @param inventoryJson Serialized inventory JSON string
     * @return CompletableFuture that completes when save is done
     */
    public CompletableFuture<Void> savePlayerInventoryAsync(UUID uuid, String playerName, String inventoryJson) {
        return CompletableFuture.runAsync(() -> {
            savePlayerInventorySync(uuid, playerName, inventoryJson);
        }, executor);
    }
    
    /**
     * Save player inventory synchronously.
     * Use this for server shutdown scenarios where async may not complete.
     * 
     * @param uuid Player UUID
     * @param playerName Player display name
     * @param inventoryJson Serialized inventory JSON string
     */
    public synchronized void savePlayerInventorySync(UUID uuid, String playerName, String inventoryJson) {
        if (connection == null) return;
        
        try (PreparedStatement stmt = connection.prepareStatement(UPSERT_SQL)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, inventoryJson);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setInt(5, 0); // is_dirty = false
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log error but don't throw to avoid disrupting server
            System.err.println("[OPanel] Failed to save inventory for " + playerName + ": " + e.getMessage());
        }
    }
    
    /**
     * Get offline player inventory data asynchronously.
     * 
     * @param uuid Player UUID
     * @return CompletableFuture with PlayerData, or null if not found
     */
    public CompletableFuture<PlayerData> getOfflineInventory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            return getOfflineInventorySync(uuid);
        }, executor);
    }
    
    /**
     * Get offline player inventory data synchronously.
     * 
     * @param uuid Player UUID
     * @return PlayerData or null if not found
     */
    public synchronized PlayerData getOfflineInventorySync(UUID uuid) {
        if (connection == null) return null;
        
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_SQL)) {
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                        uuid,
                        rs.getString("player_name"),
                        rs.getString("inventory_json"),
                        rs.getLong("last_updated"),
                        rs.getInt("is_dirty") == 1
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[OPanel] Failed to load inventory for " + uuid + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Mark a player record as dirty (has pending web changes).
     * 
     * @param uuid Player UUID
     * @param dirty Whether the record has pending changes
     */
    public CompletableFuture<Void> setDirty(UUID uuid, boolean dirty) {
        return CompletableFuture.runAsync(() -> {
            if (connection == null) return;
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE player_data SET is_dirty = ? WHERE uuid = ?")) {
                stmt.setInt(1, dirty ? 1 : 0);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[OPanel] Failed to set dirty flag for " + uuid + ": " + e.getMessage());
            }
        }, executor);
    }
    
    /**
     * Close the database connection and shutdown executor.
     * Should be called in plugin onDisable().
     */
    public synchronized void close() {
        // Shutdown executor and wait for pending tasks
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close database connection
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[OPanel] Failed to close database connection: " + e.getMessage());
            }
            connection = null;
        }
    }
    
    /**
     * Check if the database is initialized and connected.
     */
    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Data class for player inventory record.
     */
    public static class PlayerData {
        private final UUID uuid;
        private final String playerName;
        private final String inventoryJson;
        private final long lastUpdated;
        private final boolean dirty;
        
        public PlayerData(UUID uuid, String playerName, String inventoryJson, long lastUpdated, boolean dirty) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.inventoryJson = inventoryJson;
            this.lastUpdated = lastUpdated;
            this.dirty = dirty;
        }
        
        public UUID getUuid() { return uuid; }
        public String getPlayerName() { return playerName; }
        public String getInventoryJson() { return inventoryJson; }
        public long getLastUpdated() { return lastUpdated; }
        public boolean isDirty() { return dirty; }
    }
}
