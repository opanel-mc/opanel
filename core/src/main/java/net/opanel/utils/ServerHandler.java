package net.opanel.utils;

import com.sun.net.httpserver.*;
import net.opanel.OPanel;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ServerHandler implements HttpHandler {
    protected final OPanel plugin;

    public ServerHandler(OPanel plugin) {
        this.plugin = plugin;
    }

    protected void sendResponse(HttpExchange req, String msg) {
        sendResponse(req, 200, msg);
    }

    protected void sendResponse(HttpExchange req, int code, String msg) {
        try {
            req.sendResponseHeaders(code, msg.length());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try(OutputStream os = req.getResponseBody()) {
            os.write(msg.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
