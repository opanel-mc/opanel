package net.opanel.endpoint;

import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsMessageContext;
import net.opanel.OPanel;
import net.opanel.event.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for player inventory synchronization.
 * Provides real-time inventory updates to connected clients.
 */
public class InventoryEndpoint extends BaseEndpoint {
    private static class InventoryPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String FETCH = "fetch";
        public static final String UPDATE = "update";
        public static final String PLAYER_JOIN = "player-join";
        public static final String PLAYER_LEAVE = "player-leave";

        public InventoryPacket(String type, D data) {
            super(type, data);
        }
    }

    // Cache for player inventory data
    private final ConcurrentHashMap<String, Map<String, Object>> inventoryCache = new ConcurrentHashMap<>();

    public InventoryEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        super(app, ws, plugin);

        // Listen for inventory change events
        EventManager.get().on(EventType.PLAYER_INVENTORY_CHANGE, (OPanelPlayerInventoryChangeEvent event) -> {
            String uuid = event.getPlayer().getUUID();
            Map<String, Object> inventoryData = event.getInventoryData();
            
            // Update cache
            inventoryCache.put(uuid, inventoryData);

            // Broadcast update
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("uuid", uuid);
            payload.put("name", event.getPlayer().getName());
            payload.put("inventory", inventoryData);
            broadcast(new InventoryPacket<>(InventoryPacket.UPDATE, payload));
        });

        // Listen for player join events
        EventManager.get().on(EventType.PLAYER_JOIN, (OPanelPlayerJoinEvent event) -> {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("uuid", event.getPlayer().getUUID());
            payload.put("name", event.getPlayer().getName());
            broadcast(new InventoryPacket<>(InventoryPacket.PLAYER_JOIN, payload));
        });

        // Listen for player leave events
        EventManager.get().on(EventType.PLAYER_LEAVE, (OPanelPlayerLeaveEvent event) -> {
            String uuid = event.getPlayer().getUUID();
            inventoryCache.remove(uuid);

            HashMap<String, Object> payload = new HashMap<>();
            payload.put("uuid", uuid);
            payload.put("name", event.getPlayer().getName());
            broadcast(new InventoryPacket<>(InventoryPacket.PLAYER_LEAVE, payload));
        });
    }

    @Override
    public void onConnect(WsMessageContext ctx) {
        // Send cached inventory data on connect
        ctx.send(new InventoryPacket<>(InventoryPacket.INIT, inventoryCache));

        // Subscribe to fetch requests for specific player
        subscribe(ctx.session, InventoryPacket.FETCH, String.class, (msgCtx, playerUuid) -> {
            if (playerUuid != null && inventoryCache.containsKey(playerUuid)) {
                HashMap<String, Object> payload = new HashMap<>();
                payload.put("uuid", playerUuid);
                payload.put("inventory", inventoryCache.get(playerUuid));
                msgCtx.send(new InventoryPacket<>(InventoryPacket.UPDATE, payload));
            }
        });
    }

    /**
     * Update the inventory cache directly (called by sync task).
     */
    public void updateCache(String uuid, Map<String, Object> inventoryData) {
        inventoryCache.put(uuid, inventoryData);
    }

    /**
     * Get cached inventory data for a player.
     */
    public Map<String, Object> getCachedInventory(String uuid) {
        return inventoryCache.get(uuid);
    }
}
