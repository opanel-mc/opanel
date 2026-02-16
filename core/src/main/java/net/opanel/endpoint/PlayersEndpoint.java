package net.opanel.endpoint;

import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.*;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PlayersEndpoint extends BaseEndpoint {
    private static class PlayersPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String FETCH = "fetch";
        public static final String JOIN = "join";
        public static final String LEAVE = "leave";
        public static final String GAMEMODE_CHANGE = "gamemode-change";

        public PlayersPacket(String type, D data) {
            super(type, data);
        }
    }

    private final ConcurrentHashMap<String, Long> joinTimeMap = new ConcurrentHashMap<>();

    public PlayersEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        super(app, ws, plugin);

        EventManager.get().on(EventType.PLAYER_JOIN, (OPanelPlayerJoinEvent event) -> {
            try {
                final long joinTime = System.currentTimeMillis();
                joinTimeMap.put(event.getPlayer().getUUID(), joinTime);

                List<String> whitelistedNames = server.getWhitelist().getNames();
                broadcast(new PlayersPacket<>(PlayersPacket.JOIN, serializePlayer(event.getPlayer(), whitelistedNames, joinTime)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        EventManager.get().on(EventType.PLAYER_LEAVE, (OPanelPlayerLeaveEvent event) -> {
            try {
                joinTimeMap.remove(event.getPlayer().getUUID());

                List<String> whitelistedNames = server.getWhitelist().getNames();
                HashMap<String, Object> playerInfo = serializePlayer(event.getPlayer(), whitelistedNames, null);
                playerInfo.put("isOnline", false);
                broadcast(new PlayersPacket<>(PlayersPacket.LEAVE, playerInfo));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        EventManager.get().on(EventType.PLAYER_GAMEMODE_CHANGE, (OPanelPlayerGameModeChangeEvent event) -> {
            try {
                List<String> whitelistedNames = server.getWhitelist().getNames();
                HashMap<String, Object> playerInfo = serializePlayer(event.getPlayer(), whitelistedNames, null);
                playerInfo.put("gamemode", event.getGameMode().getName());
                broadcast(new PlayersPacket<>(PlayersPacket.GAMEMODE_CHANGE, playerInfo));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onConnect(WsContext ctx) {
        sendPlayerList(ctx);

        subscribe(ctx.session, PlayersPacket.FETCH, this::sendPlayerList);
    }

    private void sendPlayerList(WsContext ctx) {
        try {
            List<String> whitelistedNames = server.getWhitelist().getNames();
            List<HashMap<String, Object>> players = server.getPlayers().stream()
                    .map(player -> (
                            serializePlayer(player, whitelistedNames, player.isOnline() ? joinTimeMap.get(player.getUUID()) : null)
                    ))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            ctx.send(new PlayersPacket<>(PlayersPacket.INIT, players));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, Object> serializePlayer(OPanelPlayer player, List<String> whitelistedNames, Long joinTime) {
        HashMap<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("name", player.getName());
        playerInfo.put("uuid", player.getUUID());
        playerInfo.put("isOnline", player.isOnline());
        playerInfo.put("isOp", player.isOp());
        playerInfo.put("isBanned", player.isBanned());
        playerInfo.put("gamemode", player.getGameMode().getName());
        final String banReason = player.getBanReason();
        if(banReason != null) playerInfo.put("banReason", Utils.stringToBase64(banReason));
        if(server.isWhitelistEnabled()) playerInfo.put("isWhitelisted", whitelistedNames.contains(player.getName()));
        if(player.isOnline()) {
            playerInfo.put("ping", player.getPing());
            playerInfo.put("ip", player.getAddress().getHostAddress());
            playerInfo.put("joinTime", joinTime);
        }
        return playerInfo;
    }
}
