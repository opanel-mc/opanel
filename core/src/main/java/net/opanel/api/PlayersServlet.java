package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.web.BaseServlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayersServlet extends BaseServlet {
    public static final String route = "/api/players";

    public PlayersServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final OPanelServer server = plugin.getServer();

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("maxPlayerCount", server.getMaxPlayerCount());

        List<HashMap<String, Object>> players = new ArrayList<>();
        for(OPanelPlayer player : server.getPlayers()) {
            HashMap<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getName());
            playerInfo.put("uuid", player.getUUID());
            playerInfo.put("isOnline", player.isOnline());
            playerInfo.put("isOp", player.isOp());
            playerInfo.put("isBanned", player.isBanned());
            playerInfo.put("gamemode", player.getGameMode().getName());
            players.add(playerInfo);
        }
        obj.put("players", players);

        sendResponse(res, obj);
    }
}
