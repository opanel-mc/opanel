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

public class GamerulesServlet extends BaseServlet {
    public static final String route = "/api/gamerules";

    public GamerulesServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(req.getMethod().equals("OPTIONS")) {
            sendResponse(res, HttpServletResponse.SC_OK);
            return;
        }

        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final OPanelServer server = plugin.getServer();

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("gamerules", server.getGamerules());

        sendResponse(res, obj);
    }
}
