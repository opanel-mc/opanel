package net.opanel.api;

import com.sun.net.httpserver.HttpExchange;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.utils.ServerHandler;

public class IconHandler extends ServerHandler {
    public static final String route = "/api/icon";

    public IconHandler(OPanel plugin) {
        super(plugin);
    }

    @Override
    public void handle(HttpExchange req) {
        final OPanelServer server = plugin.getServer();
        byte[] favicon = server.getFavicon();
        if(favicon == null) {
            sendResponse(req, 404);
        }

        sendContentResponse(req, favicon, "image/png");
    }
}
