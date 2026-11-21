package net.opanel.endpoint;

import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PlayersEndpoint extends BaseEndpoint {
    private static final long MOVE_BROADCAST_INTERVAL_MS = 1000L;

    private static class PlayersPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String FETCH = "fetch";
        public static final String FETCH_PING = "fetch-ping";
        public static final String PING = "ping";
        public static final String JOIN = "join";
        public static final String LEAVE = "leave";
        public static final String MOVE = "move";
        public static final String GAMEMODE_CHANGE = "gamemode-change";

        public PlayersPacket(String type, D data) {
            super(type, data);
        }
    }

    private final ConcurrentHashMap<String, Long> joinTimeMap = new ConcurrentHashMap<>();
    private final Object pendingPlayerMovesLock = new Object();
    private final HashMap<String, HashMap<String, Object>> pendingPlayerMoves = new HashMap<>();
    private final ScheduledExecutorService moveBroadcastScheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "opanel-player-move-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private final Consumer<OPanelPlayerJoinEvent> joinListener;
    private final Consumer<OPanelPlayerLeaveEvent> leaveListener;
    private final Consumer<OPanelPlayerMoveEvent> moveListener;
    private final Consumer<OPanelPlayerGameModeChangeEvent> gamemodeChangeListener;

    public PlayersEndpoint(WsConfig ws, OPanel plugin) {
        super(ws, plugin);

        joinListener = (OPanelPlayerJoinEvent event) -> {
            try {
                final long joinTime = System.currentTimeMillis();
                joinTimeMap.put(event.getPlayer().getUUID(), joinTime);

                List<String> whitelistedNames = server.getWhitelist().getNames();
                broadcast(new PlayersPacket<>(PlayersPacket.JOIN, event.getPlayer().serialize(server.isWhitelistEnabled(), whitelistedNames, joinTime)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        leaveListener = (OPanelPlayerLeaveEvent event) -> {
            try {
                String uuid = event.getPlayer().getUUID();
                joinTimeMap.remove(uuid);
                synchronized(pendingPlayerMovesLock) {
                    pendingPlayerMoves.remove(uuid);
                }

                List<String> whitelistedNames = server.getWhitelist().getNames();
                HashMap<String, Object> playerInfo = event.getPlayer().serialize(server.isWhitelistEnabled(), whitelistedNames, null);
                playerInfo.put("isOnline", false);
                broadcast(new PlayersPacket<>(PlayersPacket.LEAVE, playerInfo));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        moveListener = (OPanelPlayerMoveEvent event) -> {
            HashMap<String, Object> playerInfo = new HashMap<>();
            String uuid = event.getPlayer().getUUID();
            playerInfo.put("uuid", uuid);
            playerInfo.put("name", event.getPlayer().getName());

            playerInfo.put("position", event.getPosition().serialize());

            synchronized(pendingPlayerMovesLock) {
                pendingPlayerMoves.put(uuid, playerInfo);
            }
        };

        gamemodeChangeListener = (OPanelPlayerGameModeChangeEvent event) -> {
            try {
                List<String> whitelistedNames = server.getWhitelist().getNames();
                HashMap<String, Object> playerInfo = event.getPlayer().serialize(server.isWhitelistEnabled(), whitelistedNames, null);
                playerInfo.put("gamemode", event.getGameMode().getName());
                broadcast(new PlayersPacket<>(PlayersPacket.GAMEMODE_CHANGE, playerInfo));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        EventManager.get().on(EventType.PLAYER_JOIN, joinListener);
        EventManager.get().on(EventType.PLAYER_LEAVE, leaveListener);
        EventManager.get().on(EventType.PLAYER_MOVE, moveListener);
        EventManager.get().on(EventType.PLAYER_GAMEMODE_CHANGE, gamemodeChangeListener);

        moveBroadcastScheduler.scheduleWithFixedDelay(
                this::broadcastPlayerMoves,
                MOVE_BROADCAST_INTERVAL_MS,
                MOVE_BROADCAST_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void onConnect(WsContext ctx) {
        boolean onlineOnly = Boolean.parseBoolean(ctx.queryParam("onlineOnly"));
        boolean positionOnly = Boolean.parseBoolean(ctx.queryParam("positionOnly"));
        sendPlayerList(ctx, onlineOnly, positionOnly);

        subscribe(ctx.session, PlayersPacket.FETCH, fetchCtx -> sendPlayerList(fetchCtx, onlineOnly, positionOnly));
        subscribe(ctx.session, PlayersPacket.FETCH_PING, this::sendPlayerPings);
    }

    @Override
    public void onShutdown() {
        EventManager.get().off(EventType.PLAYER_JOIN, joinListener);
        EventManager.get().off(EventType.PLAYER_LEAVE, leaveListener);
        EventManager.get().off(EventType.PLAYER_MOVE, moveListener);
        EventManager.get().off(EventType.PLAYER_GAMEMODE_CHANGE, gamemodeChangeListener);

        moveBroadcastScheduler.shutdownNow();
        synchronized(pendingPlayerMovesLock) {
            pendingPlayerMoves.clear();
        }
    }

    private void broadcastPlayerMoves() {
        List<HashMap<String, Object>> playerMoves;
        synchronized(pendingPlayerMovesLock) {
            if(pendingPlayerMoves.isEmpty()) return;

            playerMoves = new ArrayList<>(pendingPlayerMoves.values());
            pendingPlayerMoves.clear();
        }

        broadcast(new PlayersPacket<>(PlayersPacket.MOVE, playerMoves));
    }

    private void sendPlayerPings(WsContext ctx) {
        HashMap<String, Integer> pings = new HashMap<>();
        server.getOnlinePlayers().forEach(player -> pings.put(player.getUUID(), player.getPing()));
        ctx.send(new PlayersPacket<>(PlayersPacket.PING, pings));
    }

    private void sendPlayerList(WsContext ctx, boolean onlineOnly, boolean positionOnly) {
        try {
            List<OPanelPlayer> sourcePlayers = onlineOnly ? server.getOnlinePlayers() : server.getPlayers();
            List<HashMap<String, Object>> players;
            if(positionOnly) {
                players = sourcePlayers.stream()
                        .map(PlayersEndpoint::serializePlayerPosition)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            } else {
                List<String> whitelistedNames = server.getWhitelist().getNames();
                boolean whitelistEnabled = server.isWhitelistEnabled();
                players = sourcePlayers.stream()
                        .map(player -> (
                                player.serialize(whitelistEnabled, whitelistedNames, player.isOnline() ? joinTimeMap.get(player.getUUID()) : null)
                        ))
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }

            ctx.send(new PlayersPacket<>(PlayersPacket.INIT, players));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, Object> serializePlayerPosition(OPanelPlayer player) {
        HashMap<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("name", player.getName());
        playerInfo.put("uuid", player.getUUID());
        playerInfo.put("isOnline", player.isOnline());
        playerInfo.put("position", player.getPosition().serialize());
        return playerInfo;
    }
}
