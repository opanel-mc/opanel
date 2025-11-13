package net.opanel.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayersController extends BaseController {
    public PlayersController(OPanel plugin) {
        super(plugin);
    }

    public Handler getPlayers = ctx -> {
        final boolean isWhitelistEnabled = server.isWhitelistEnabled();
        try {
            final List<String> whitelistNames = server.getWhitelist().getNames();
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("maxPlayerCount", server.getMaxPlayerCount());
            obj.put("whitelist", isWhitelistEnabled);

            List<HashMap<String, Object>> players = new ArrayList<>();
            for(OPanelPlayer player : server.getPlayers()) {
                HashMap<String, Object> playerInfo = new HashMap<>();
                playerInfo.put("name", player.getName());
                playerInfo.put("uuid", player.getUUID());
                playerInfo.put("isOnline", player.isOnline());
                playerInfo.put("isOp", player.isOp());
                playerInfo.put("isBanned", player.isBanned());
                playerInfo.put("gamemode", player.getGameMode().getName());
                final String banReason = player.getBanReason();
                playerInfo.put("banReason", banReason != null ? Utils.stringToBase64(banReason) : null);
                if(isWhitelistEnabled) playerInfo.put("isWhitelisted", whitelistNames.contains(player.getName()));
                players.add(playerInfo);
            }
            obj.put("players", players);

            sendResponse(ctx, obj);
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler giveOp = ctx -> {
        OPanelPlayer player = getTargetPlayer(ctx);
        if(player == null) return;
        player.giveOp();
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler depriveOp = ctx -> {
        OPanelPlayer player = getTargetPlayer(ctx);
        if(player == null) return;
        player.depriveOp();
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler kickPlayer = ctx -> {
        OPanelPlayer player = getTargetPlayer(ctx);
        if(player == null) return;
        if(!player.isOnline()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Player is offline.");
            return;
        }
        String reason = ctx.queryParam("r");
        player.kick(reason != null ? Utils.base64ToString(reason) : null);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler banPlayer = ctx -> {
        OPanelPlayer player = getTargetPlayer(ctx);
        if(player == null) return;
        String reason = ctx.queryParam("r");
        player.ban(reason != null ? Utils.base64ToString(reason) : null);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler pardonPlayer = ctx -> {
        OPanelPlayer player = getTargetPlayer(ctx);
        if(player == null) return;
        player.pardon();
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler setGamemode = ctx -> {
        OPanelPlayer player = getTargetPlayer(ctx);
        if(player == null) return;
        String gamemode = ctx.queryParam("gm");
        if(gamemode == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Gamemode is required.");
            return;
        }
        player.setGameMode(OPanelGameMode.fromString(gamemode));
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler deletePlayerData = ctx -> {
        String uuid = ctx.queryParam("uuid");
        if(uuid == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Uuid is required.");
            return;
        }

        if(server.getPlayer(uuid) == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Player not found.");
            return;
        }

        server.removePlayerData(uuid);
        sendResponse(ctx, HttpStatus.OK);
    };

    private OPanelPlayer getTargetPlayer(Context ctx) {
        String uuid = ctx.queryParam("uuid");
        if(uuid == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Uuid is required.");
            return null;
        }

        final OPanelPlayer player = server.getPlayer(uuid);
        if(player == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Player not found.");
            return null;
        }

        return player;
    }
}
