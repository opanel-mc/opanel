package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.logger.Loggable;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LogsServlet extends BaseServlet {
    public static final String route = "/api/logs/*";

    public LogsServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final Loggable logger = plugin.logger;
        if(reqPath == null || reqPath.equals("/")) {
            try {
                HashMap<String, Object> obj = new HashMap<>();
                obj.put("logs", logger.getLogFileList());
                sendResponse(res, obj);
            } catch (IOException e) {
                sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else if(reqPath.startsWith("/")) {
            String fileName = reqPath.substring(1);
            try {
                HashMap<String, Object> obj = new HashMap<>();
                obj.put("log", logger.getLogContent(fileName));
                sendResponse(res, obj);
            } catch (IOException e) {
                sendResponse(res, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            }
        } else {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final Loggable logger = plugin.logger;
        if(reqPath == null || reqPath.equals("/") || !reqPath.startsWith("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            logger.deleteLog(reqPath.substring(1));
        } catch (IOException e) {
            sendResponse(res, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
        sendResponse(res, HttpServletResponse.SC_OK);
    }
}
