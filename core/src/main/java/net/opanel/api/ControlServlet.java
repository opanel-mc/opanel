package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.web.BaseServlet;

public class ControlServlet extends BaseServlet {
    public static final String route = "/api/control/*";

    public ControlServlet(OPanel plugin) {
        super(plugin);
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

        switch(reqPath.substring(1)) {
            case "stop" -> server.stop();
            case "reload" -> server.reload();
        }
        sendResponse(res, HttpServletResponse.SC_OK);
    }
}
