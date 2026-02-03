package net.opanel.endpoint;

import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsMessageContext;
import net.opanel.OPanel;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelPlayerInventoryChangeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InventoryEndpoint extends BaseEndpoint {
    private static class InventoryPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String FETCH = "fetch";
        public static final String UPDATE = "update";
        public static final String MOVE = "move";

        public InventoryPacket(String type, D data) {
            super(type, data);
        }
    }

    private static class InventoryFetchPayload {
        public String uuid;
    }

    private static class InventoryMovePayload {
        public String uuid;
        public List<OPanelInventory.OPanelItemStack> items;
    }

    public InventoryEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        super(app, ws, plugin);

        EventManager.get().on(EventType.PLAYER_INVENTORY_CHANGE, (OPanelPlayerInventoryChangeEvent event) -> {
            HashMap<String, Object> data = serializeInventory(event.getPlayer(), event.getInventory());
            if(data != null) {
                broadcast(new InventoryPacket<>(InventoryPacket.UPDATE, data));
            }
        });
    }

    @Override
    public void onConnect(WsMessageContext ctx) {
        sendInventoryList(ctx);

        subscribe(ctx.session, InventoryPacket.FETCH, InventoryFetchPayload.class, (msgCtx, payload) -> {
            if(payload == null || payload.uuid == null || payload.uuid.isEmpty()) {
                sendInventoryList(msgCtx);
                return;
            }

            OPanelPlayer player = server.getPlayer(payload.uuid);
            if(player == null) {
                sendErrorMessage(msgCtx, "Player not found.");
                return;
            }

            OPanelInventory inventory = player.getInventory();
            HashMap<String, Object> data = serializeInventory(player, inventory);
            msgCtx.send(new InventoryPacket<>(InventoryPacket.INIT, data));
        });

        subscribe(ctx.session, InventoryPacket.MOVE, InventoryMovePayload.class, (msgCtx, payload) -> {
            if(payload == null || payload.uuid == null) {
                sendErrorMessage(msgCtx, "Missing uuid.");
                return;
            }
            if(payload.items == null || payload.items.isEmpty()) {
                sendErrorMessage(msgCtx, "Items are required.");
                return;
            }

            OPanelPlayer player = server.getPlayer(payload.uuid);
            if(player == null) {
                sendErrorMessage(msgCtx, "Player not found.");
                return;
            }

            OPanelInventory inventory = player.getInventory();

            for(OPanelInventory.OPanelItemStack item : payload.items) {
                inventory.setItem(item);
            }

            HashMap<String, Object> data = serializeInventory(player, inventory);
            if(data != null) {
                broadcast(new InventoryPacket<>(InventoryPacket.UPDATE, data));
            }
        });
    }

    private void sendInventoryList(WsMessageContext ctx) {
        List<HashMap<String, Object>> inventories = server.getPlayers().stream()
                .map(player -> serializeInventory(player, player.getInventory()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        ctx.send(new InventoryPacket<>(InventoryPacket.INIT, inventories));
    }

    private HashMap<String, Object> serializeInventory(OPanelPlayer player, OPanelInventory inventory) {
        if(player == null || inventory == null) return null;
        HashMap<String, Object> data = new HashMap<>();
        data.put("name", player.getName());
        data.put("uuid", player.getUUID());
        data.put("isOnline", player.isOnline());
        data.put("size", inventory.getSize());
        data.put("hash", inventory.getHash());
        data.put("items", inventory.getItems());
        return data;
    }
}