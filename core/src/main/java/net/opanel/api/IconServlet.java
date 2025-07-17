package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.web.BaseServlet;

public class IconServlet extends BaseServlet {
    public static final String route = "/api/icon";

    public IconServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        final OPanelServer server = plugin.getServer();
        byte[] favicon = server.getFavicon();
        if(favicon == null) {
            sendResponse(res, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        sendContentResponse(res, favicon, "image/png");
    }
}
