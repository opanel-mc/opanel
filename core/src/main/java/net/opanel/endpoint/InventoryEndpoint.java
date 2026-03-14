package net.opanel.endpoint;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.opanel.OPanel;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelPlayerInventoryChangeEvent;
import org.eclipse.jetty.websocket.api.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class InventoryEndpoint extends BaseEndpoint {
    private static class InventoryPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String FETCH = "fetch";
        public static final String UPDATE = "update"; // client <- server

        public InventoryPacket(String type, D data) {
            super(type, data);
        }
    }

    private static final ConcurrentHashMap<String, Set<Session>> sessionsMap = new ConcurrentHashMap<>();

    // To avoid duplicated inventory listener from registering
    private boolean hasInventoryListenerRegistered = false;

    public InventoryEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        super(app, ws, plugin);
    }

    private HashMap<String, Object> serializeInventory(OPanelPlayer player, OPanelInventory inventory) {
        HashMap<String, Object> data = inventory.serialize();

        boolean canReadEnderChest = inventory.canReadEnderChest();
        boolean canWriteEnderChest = inventory.canWriteEnderChest();

        if(!canReadEnderChest) {
            data.put("enderSize", 0);
            data.put("enderHash", null);
            data.put("enderItems", Collections.emptyList());
        }

        HashMap<String, Object> capabilities = new HashMap<>();
        capabilities.put("readEnderChest", canReadEnderChest);
        capabilities.put("writeEnderChest", canWriteEnderChest);
        data.put("capabilities", capabilities);

        return data;
    }

    @Override
    public void onConnect(WsContext ctx) {
        final String uuid = ctx.pathParam("uuid");
        if(uuid.isEmpty()) {
            sendErrorMessage(ctx, HttpStatus.UNAUTHORIZED);
            ctx.closeSession(1008, "Missing uuid.");
            return;
        }

        OPanelPlayer player = server.getPlayer(uuid);
        if(player == null) {
            sendErrorMessage(ctx, HttpStatus.NOT_FOUND);
            ctx.closeSession(1008, "Player not found.");
            return;
        }

        Set<Session> sessions = sessionsMap.computeIfAbsent(uuid, k -> new CopyOnWriteArraySet<>());
        sessions.add(ctx.session);

        // Send initial inventory data
        ctx.send(new InventoryPacket<>(InventoryPacket.INIT, serializeInventory(player, player.getInventory())));

        subscribe(ctx.session, InventoryPacket.FETCH, msgCtx -> {
            OPanelPlayer currentPlayer = server.getPlayer(uuid);
            if(currentPlayer == null) {
                sendErrorMessage(msgCtx, HttpStatus.NOT_FOUND);
                return;
            }
            msgCtx.send(new InventoryPacket<>(InventoryPacket.INIT, serializeInventory(currentPlayer, currentPlayer.getInventory())));
        });

        subscribe(ctx.session, InventoryPacket.UPDATE, OPanelInventory.OPanelItemStack.class, (msgCtx, item) -> {
            if(item == null) {
                sendErrorMessage(msgCtx, HttpStatus.BAD_REQUEST);
                return;
            }

            OPanelPlayer currentPlayer = server.getPlayer(uuid);
            if(currentPlayer == null) {
                sendErrorMessage(msgCtx, HttpStatus.NOT_FOUND);
                return;
            }

            OPanelInventory currentInventory = currentPlayer.getInventory();
            String container = item.container == null ? "main" : item.container;
            if(!container.equals("main") && !container.equals("ender")) {
                sendErrorMessage(msgCtx, HttpStatus.BAD_REQUEST);
                return;
            }
            OPanelInventory.OPanelItemStack targetItem = new OPanelInventory.OPanelItemStack(item.slot, item.id, item.count, item.snbt, container);

            try {
                if(container.equals("ender")) {
                    if(!currentInventory.canWriteEnderChest()) {
                        sendErrorMessage(msgCtx, HttpStatus.FORBIDDEN);
                        return;
                    }
                    if(item.slot < 0 || item.slot >= currentInventory.getEnderSize()) {
                        sendErrorMessage(msgCtx, HttpStatus.BAD_REQUEST);
                        return;
                    }
                    currentInventory.setEnderItem(targetItem);
                } else {
                    currentInventory.setItem(targetItem);
                }
            } catch (Exception e) {
                sendErrorMessage(msgCtx, HttpStatus.BAD_REQUEST);
                return;
            }

            HashMap<String, Object> updatedData = serializeInventory(currentPlayer, currentInventory);
            if(updatedData != null) {
                Set<Session> listenedSessions = sessionsMap.get(uuid);
                if(listenedSessions != null) {
                    for(Session session : listenedSessions) {
                        if(!session.isOpen()) {
                            listenedSessions.remove(session);
                            continue;
                        }
                        sendMessage(session, new InventoryPacket<>(InventoryPacket.UPDATE, updatedData));
                    }
                }
            }
        });

        if(!hasInventoryListenerRegistered) {
            EventManager.get().on(EventType.PLAYER_INVENTORY_CHANGE, (OPanelPlayerInventoryChangeEvent event) -> {
                final String targetUuid = event.getPlayer().getUUID();
                if(!sessionsMap.containsKey(targetUuid)) return;

                HashMap<String, Object> data = serializeInventory(event.getPlayer(), event.getInventory());
                Set<Session> listenedSessions = sessionsMap.get(targetUuid);
                for(Session session : listenedSessions) {
                    if(!session.isOpen()) {
                        listenedSessions.remove(session);
                        continue;
                    }
                    sendMessage(session, new InventoryPacket<>(InventoryPacket.UPDATE, data));
                }
            });
            hasInventoryListenerRegistered = true;
        }
    }
}
