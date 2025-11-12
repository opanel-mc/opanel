package net.opanel.web;

import com.google.gson.Gson;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class BaseController {
    protected final OPanel plugin;
    protected final OPanelServer server;

    public BaseController(OPanel plugin) {
        this.plugin = plugin;
        server = plugin.getServer();
    }

    protected void sendResponse(Context ctx, HttpStatus status) {
        sendResponse(ctx, status, status.getMessage());
    }

    protected void sendResponse(Context ctx, HttpStatus status, String msg) {
        ctx.status(status);

        HashMap<String, Object> jsonObj = new HashMap<>();
        jsonObj.put("code", status.getCode());
        jsonObj.put("error", msg);
        ctx.json(jsonObj);
    }

    protected void sendResponse(Context ctx, HashMap<String, Object> jsonObj) {
        HttpStatus okStatus = HttpStatus.OK;

        jsonObj.put("code", okStatus.getCode());
        jsonObj.put("error", okStatus.getMessage());
        ctx.json(jsonObj);
    }
}
