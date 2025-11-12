package net.opanel.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.OPanelPlayer;
import net.opanel.OPanelServer;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;
import net.opanel.web.JwtManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayersController extends BaseController {
    public static final String route = "/api/players";

    public PlayersController(OPanel plugin) {
        super(plugin);
    }

    public Handler getPlayers = ctx -> {
        if (!authCookie(ctx)) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
            return;
        }

        String whitelistParam = ctx.queryParam("whitelist");
        boolean whitelistOnly = "true".equals(whitelistParam);

        List<HashMap<String, Object>> playersList = new ArrayList<>();
        OPanelServer server = plugin.getServer();

        for (OPanelPlayer player : server.getPlayers()) {
            if (whitelistOnly && !player.isWhitelisted()) {
                continue;
            }

            HashMap<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("uuid", player.getUniqueId().toString());
            playerInfo.put("name", player.getName());
            playerInfo.put("displayName", player.getDisplayName());
            playerInfo.put("address", player.getAddress() != null ? player.getAddress().getHostString() : null);
            playerInfo.put("op", player.isOp());
            playerInfo.put("whitelisted", player.isWhitelisted());
            playerInfo.put("banned", player.isBanned());
            playerInfo.put("online", player.isOnline());

            if (player.isOnline()) {
                playerInfo.put("gamemode", player.getGameMode().name());
            }

            playersList.add(playerInfo);
        }

        HashMap<String, Object> response = new HashMap<>();
        response.put("players", playersList);
        sendResponse(ctx, response);
    };

    public Handler handlePlayerAction = ctx -> {
        if (!authCookie(ctx)) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
            return;
        }

        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if (reqBody.uuid == null || reqBody.action == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "UUID or action is missing.");
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(reqBody.uuid);
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid UUID format.");
            return;
        }

        OPanelPlayer player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Player not found.");
            return;
        }

        String action = reqBody.action;
        String reason = reqBody.reason != null ? Utils.base64Decode(reqBody.reason) : "";

        switch (action) {
            case "op":
                player.setOp(true);
                break;
            case "deop":
                player.setOp(false);
                break;
            case "kick":
                if (!player.isOnline()) {
                    sendResponse(ctx, HttpStatus.BAD_REQUEST, "Player is not online.");
                    return;
                }
                player.kick(reason);
                break;
            case "ban":
                player.ban(reason);
                if (player.isOnline()) {
                    player.kick("Banned: " + reason);
                }
                break;
            case "pardon":
                player.pardon();
                break;
            case "gamemode":
                if (reqBody.gamemode == null) {
                    sendResponse(ctx, HttpStatus.BAD_REQUEST, "Gamemode is required.");
                    return;
                }
                try {
                    OPanelPlayer.GameMode gameMode = OPanelPlayer.GameMode.valueOf(reqBody.gamemode.toUpperCase());
                    player.setGameMode(gameMode);
                } catch (IllegalArgumentException e) {
                    sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid gamemode.");
                    return;
                }
                break;
            default:
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid action.");
                return;
        }

        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler deletePlayer = ctx -> {
        if (!authCookie(ctx)) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
            return;
        }

        String uuidParam = ctx.queryParam("uuid");
        if (uuidParam == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "UUID is missing.");
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidParam);
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid UUID format.");
            return;
        }

        OPanelPlayer player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Player not found.");
            return;
        }

        plugin.getServer().removePlayerData(player);
        sendResponse(ctx, HttpStatus.OK);
    };

    private boolean authCookie(Context ctx) {
        String authCookie = ctx.cookie("auth");
        if (authCookie == null) {
            return false;
        }
        return JwtManager.validateToken(authCookie, plugin.getConfig().salt);
    }

    private static class RequestBodyType {
        public String uuid;
        public String action;
        public String reason;
        public String gamemode;
    }
}