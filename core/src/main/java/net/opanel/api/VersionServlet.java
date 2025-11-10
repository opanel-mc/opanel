package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.web.BaseServlet;

import java.util.HashMap;

public class VersionServlet extends BaseServlet {
    public static final String route = "/api/version";

    public VersionServlet(OPanel plugin) {
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
        obj.put("serverType", server.getServerType().getName());
        obj.put("version", server.getVersion());
        sendResponse(res, obj);
    }
}
