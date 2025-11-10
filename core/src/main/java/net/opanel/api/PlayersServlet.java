package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayersServlet extends BaseServlet {
    public static final String route = "/api/players/*";

    public PlayersServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();

        if(reqPath != null && !reqPath.equals("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

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

            sendResponse(res, obj);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();

        if(reqPath == null || reqPath.equals("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String uuid = req.getParameter("uuid");
        String reason = req.getParameter("r"); // base64, only for `kick` and `ban`
        String gamemode = req.getParameter("gm"); // only for `gamemode`
        if(uuid == null) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final OPanelPlayer player = server.getPlayer(uuid);
        if(player == null) {
            sendResponse(res, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        switch(reqPath.substring(1)) {
            case "op" -> player.giveOp();
            case "deop" -> player.depriveOp();
            case "kick" -> {
                if(!player.isOnline()) {
                    sendResponse(res, HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                player.kick(reason != null ? Utils.base64ToString(reason) : null);
            }
            case "ban" -> player.ban(reason != null ? Utils.base64ToString(reason) : null);
            case "pardon" -> player.pardon();
            case "gamemode" -> {
                if(gamemode == null) {
                    sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                player.setGameMode(OPanelGameMode.fromString(gamemode));
            }
        }

        sendResponse(res, HttpServletResponse.SC_OK);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final OPanelServer server = plugin.getServer();

        String uuid = req.getParameter("uuid");
        if(uuid == null) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if(server.getPlayer(uuid) == null) {
            sendResponse(res, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        server.removePlayerData(uuid);
        sendResponse(res, HttpServletResponse.SC_OK);
    }
}
