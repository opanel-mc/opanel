package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.web.BaseServlet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class InfoServlet extends BaseServlet {
    public static final String route = "/api/info";

    public InfoServlet(OPanel plugin) {
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
        obj.put("favicon", server.getFavicon() != null ? IconServlet.route : null);
        obj.put("motd", Base64.getEncoder().encodeToString(server.getMotd().getBytes(StandardCharsets.UTF_8)));
        obj.put("ip", server.getIP());
        obj.put("port", server.getPort());
        obj.put("maxPlayerCount", server.getMaxPlayerCount());

        List<HashMap<String, Object>> players = new ArrayList<>();
        for(OPanelPlayer player : server.getOnlinePlayers()) {
            HashMap<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getName());
            playerInfo.put("uuid", player.getUUID());
            playerInfo.put("isOp", player.isOp());
            playerInfo.put("gamemode", player.getGameMode().getName());
            players.add(playerInfo);
        }
        obj.put("onlinePlayers", players);

        sendResponse(res, obj);
    }
}
