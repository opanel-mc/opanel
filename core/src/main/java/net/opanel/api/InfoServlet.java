package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.utils.TPS;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class InfoServlet extends BaseServlet {
    public static final String route = "/api/info/*";

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
        obj.put("version", server.getVersion());
        obj.put("port", server.getPort());
        obj.put("maxPlayerCount", server.getMaxPlayerCount());
        obj.put("whitelist", server.isWhitelistEnabled());
        obj.put("uptime", plugin.getUptimer().getCurrent());

        HashMap<String, Object> ingameTimeObj = new HashMap<>();
        ingameTimeObj.put("current", server.getIngameTime());
        ingameTimeObj.put("doDaylightCycle", server.getGamerules().get("doDaylightCycle"));
        ingameTimeObj.put("paused", TPS.isPaused());
        ingameTimeObj.put("mspt", TPS.getRecentMSPT());
        obj.put("ingameTime", ingameTimeObj);

        List<HashMap<String, Object>> players = server.getOnlinePlayers().stream()
                .map(player -> {
                    HashMap<String, Object> playerInfo = new HashMap<>();
                    playerInfo.put("name", player.getName());
                    playerInfo.put("uuid", player.getUUID());
                    playerInfo.put("gamemode", player.getGameMode().getName());
                    playerInfo.put("ping", player.getPing());
                    return playerInfo;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        obj.put("onlinePlayers", players);

        sendResponse(res, obj);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();

        if(reqPath.substring(1).equals("motd")) {
            try {
                String motd = getRequestBody(req, String.class);
                if(motd == null || motd.trim().isEmpty()) {
                    sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                String decodedMotd = new String(Base64.getDecoder().decode(motd), StandardCharsets.UTF_8);
                server.setMotd(decodedMotd);
                sendResponse(res, HttpServletResponse.SC_OK);
            } catch (IOException e) {
                plugin.logger.error("Failed to update MOTD: " + e.getMessage());
                sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IllegalArgumentException e) {
                plugin.logger.error("Invalid Base64 encoding in MOTD: " + e.getMessage());
                sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
