package net.opanel.api;

import com.sun.net.httpserver.HttpExchange;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.utils.ServerHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayersHandler extends ServerHandler {
    public static final String route = "/api/players";

    public PlayersHandler(OPanel plugin) {
        super(plugin);
    }

    @Override
    public void handle(HttpExchange req) {
        if(req.getRequestMethod().equals("OPTIONS")) {
            sendResponse(req, 200);
            return;
        }

        if(!authCookie(req)) {
            sendResponse(req, 401);
            return;
        }

        final OPanelServer server = plugin.getServer();

        HashMap<String, Object> res = new HashMap<>();
        List<HashMap<String, Object>> players = new ArrayList<>();
        for(OPanelPlayer player : server.getPlayers()) {
            HashMap<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getName());
            playerInfo.put("uuid", player.getUUID());
            playerInfo.put("isOp", player.isOp());
            playerInfo.put("gameMode", player.getGameMode().getName());
            players.add(playerInfo);
        }
        res.put("players", players);

        sendResponse(req, res);
    }
}
