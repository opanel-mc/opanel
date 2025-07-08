package net.opanel.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.utils.ServerHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InfoHandler extends ServerHandler {
    public static final String route = "/api/info";

    public InfoHandler(OPanel plugin) {
        super(plugin);
    }

    @Override
    public void handle(HttpExchange req) {
        if(!authCookie(req)) {
            sendResponse(req, 401);
            return;
        }

        final OPanelServer server = plugin.getServer();

        HashMap<String, Object> res = new HashMap<>();
        res.put("favicon", server.getFavicon());
        res.put("motd", server.getMotd());
        res.put("ip", server.getIP());
        res.put("port", server.getPort());

        List<HashMap<String, Object>> players = new ArrayList<>();
        for(OPanelPlayer player : plugin.getServer().getPlayers()) {
            HashMap<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getName());
            playerInfo.put("uuid", player.getUUID());
            playerInfo.put("isOp", player.isOp());
            players.add(playerInfo);
        }
        res.put("players", players);

        sendResponse(req, res);
    }
}
