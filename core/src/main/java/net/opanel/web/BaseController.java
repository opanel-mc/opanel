package net.opanel.web;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;

import java.io.*;
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
        ctx.status(okStatus);

        jsonObj.put("code", okStatus.getCode());
        jsonObj.put("error", "");
        ctx.json(jsonObj);
    }

    protected void sendContent(Context ctx, byte[] bytes, String contentType) {
        ctx.status(HttpStatus.OK);

        try(InputStream is = new ByteArrayInputStream(bytes)) {
            ctx.writeSeekableStream(is, contentType);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
