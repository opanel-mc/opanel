package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import net.opanel.web.BaseServlet;

import java.net.UnknownHostException;
import java.util.HashMap;

public class BannedIpsServlet extends BaseServlet {
    public static final String route = "/api/banned-ips/*";

    public BannedIpsServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();

        if(reqPath != null && !reqPath.equals("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("bannedIps", server.getBannedIps());
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

        if(reqPath == null || reqPath.equals("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String ip = req.getParameter("ip");
        if(ip == null || !Utils.validateIpv4Address(ip)) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            switch(reqPath.substring(1)) {
                case "add" -> server.banIp(ip);
                case "remove" -> server.pardonIp(ip);
            }
            sendResponse(res, HttpServletResponse.SC_OK);
        } catch (UnknownHostException e) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
