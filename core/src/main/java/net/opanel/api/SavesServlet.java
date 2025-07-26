package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelSave;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SavesServlet extends BaseServlet {
    public static final String route = "/api/saves/*";

    public SavesServlet(OPanel plugin) {
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

        List<HashMap<String, Object>> saves = new ArrayList<>();
        for(OPanelSave save : server.getSaves()) {
            HashMap<String, Object> saveInfo = new HashMap<>();
            saveInfo.put("name", save.getName());
            saveInfo.put("displayName", save.getDisplayName());
            saveInfo.put("path", save.getPath().toString());
            saveInfo.put("isCurrent", save.isCurrent());
            saveInfo.put("defaultGameMode", save.getDefaultGameMode().getName());
            saves.add(saveInfo);
        }
        obj.put("saves", saves);

        sendResponse(res, obj);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();

        if(reqPath == null || reqPath.equals("/") || !reqPath.startsWith("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String saveName = reqPath.substring(1);
        OPanelSave save = server.getSave(saveName);
        if(save.isCurrent()) {
            sendResponse(res, HttpServletResponse.SC_FORBIDDEN, "The specified save is currently running on the server.");
            return;
        }

        try {
            save.delete();
            sendResponse(res, HttpServletResponse.SC_OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
