package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.util.HashMap;

public class ControlServlet extends BaseServlet {
    public static final String route = "/api/control/*";

    public ControlServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        HashMap<String, Object> obj = new HashMap<>();

        if(reqPath == null || reqPath.equals("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch(reqPath.substring(1)) {
            case "properties" -> {
                try {
                    obj.put("properties", OPanelServer.getPropertiesContent());
                    sendResponse(res, obj);
                } catch (IOException e) {
                    e.printStackTrace();
                    sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                return;
            }
        }
        sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
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
            case "properties" -> {
                try {
                    String newContent = getRequestBody(req, String.class);
                    OPanelServer.writePropertiesContent(newContent);
                } catch (IOException e) {
                    e.printStackTrace();
                    sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }
            }
            case "world" -> {
                String saveName = req.getParameter("save");
                if(saveName == null) {
                    sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                OPanelSave save = server.getSave(saveName);
                if(save == null) {
                    sendResponse(res, HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                try {
                    save.setToCurrent();
                } catch (IOException e) {
                    e.printStackTrace();
                    sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
            default -> {
                sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        sendResponse(res, HttpServletResponse.SC_OK);
    }
}
