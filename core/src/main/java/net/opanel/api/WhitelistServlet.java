package net.opanel.api;

import com.google.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class WhitelistServlet extends BaseServlet {
    public static final String route = "/api/whitelist/*";

    public WhitelistServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final OPanelServer server = plugin.getServer();
        final OPanelWhitelist whitelist = server.getWhitelist();

        try {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("whitelist", whitelist.getEntries());
            sendResponse(res, obj);
        } catch(IOException e) {
            e.printStackTrace();
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();
        final OPanelWhitelist whitelist = server.getWhitelist();

        String name = req.getParameter("name");
        String uuid = req.getParameter("uuid");

        try {
            switch(reqPath.substring(1)) {
                case "enable" -> server.setWhitelistEnabled(true);
                case "disable" -> server.setWhitelistEnabled(false);
                case "write" -> whitelist.write(getRequestBody(req, new TypeToken<List<OPanelWhitelist.OPanelWhitelistEntry>>() {}.getType()));
                case "add" -> {
                    if(name == null || uuid == null) {
                        sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    whitelist.add(new OPanelWhitelist.OPanelWhitelistEntry(name, uuid));
                }
                case "remove" -> {
                    if(name == null || uuid == null) {
                        sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    whitelist.remove(new OPanelWhitelist.OPanelWhitelistEntry(name, uuid));
                }
            }
            sendResponse(res, HttpServletResponse.SC_OK);
        } catch(IOException e) {
            e.printStackTrace();
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
