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

import java.util.HashMap;
import java.util.List;

public class InventoryEndpoint extends BaseEndpoint {
    private static class InventoryPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String FETCH = "fetch";
        public static final String UPDATE = "update"; // client <- server

        public InventoryPacket(String type, D data) {
            super(type, data);
        }
    }

    // To avoid duplicated inventory listener from registering
    private boolean hasInventoryListenerRegistered = false;

    public InventoryEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        super(app, ws, plugin);
    }

    @Override
    public void onConnect(WsMessageContext ctx) {
        final String uuid = ctx.pathParam("uuid");
        if(uuid.isEmpty()) {
            sendErrorMessage(ctx, "Missing uuid in path.");
            ctx.closeSession(1008, "Missing uuid.");
            return;
        }

        OPanelPlayer player = server.getPlayer(uuid);
        if(player == null) {
            sendErrorMessage(ctx, "Player not found.");
            ctx.closeSession(1008, "Player not found.");
            return;
        }

        // Send initial inventory data
        ctx.send(new InventoryPacket<>(InventoryPacket.INIT, player.getInventory().serialize()));

        subscribe(ctx.session, InventoryPacket.FETCH, msgCtx -> {
            msgCtx.send(new InventoryPacket<>(InventoryPacket.INIT, player.getInventory().serialize()));
        });

        subscribe(ctx.session, InventoryPacket.UPDATE, OPanelInventory.OPanelItemStack.class, (msgCtx, item) -> {
            if(item == null) {
                sendErrorMessage(msgCtx, "Items are required.");
                return;
            }

            OPanelPlayer currentPlayer = server.getPlayer(uuid);
            if(currentPlayer == null) {
                sendErrorMessage(msgCtx, "Player not found.");
                return;
            }

            OPanelInventory currentInventory = currentPlayer.getInventory();
            currentInventory.setItem(item);

            HashMap<String, Object> updatedData = currentInventory.serialize();
            if(updatedData != null) {
                broadcast(new InventoryPacket<>(InventoryPacket.UPDATE, updatedData));
            }
        });

        if(!hasInventoryListenerRegistered) {
            EventManager.get().on(EventType.PLAYER_INVENTORY_CHANGE, (OPanelPlayerInventoryChangeEvent event) -> {
                HashMap<String, Object> data = event.getInventory().serialize();
                if(event.getPlayer().getUUID().equals(uuid) && data != null) {
                    broadcast(new InventoryPacket<>(InventoryPacket.UPDATE, data));
                }
            });
            hasInventoryListenerRegistered = true;
        }
    }
}