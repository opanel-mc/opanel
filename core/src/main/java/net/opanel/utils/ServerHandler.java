package net.opanel.utils;

import com.google.gson.Gson;
import com.sun.net.httpserver.*;
import net.opanel.OPanel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ServerHandler implements HttpHandler {
    protected final OPanel plugin;

    public ServerHandler(OPanel plugin) {
        this.plugin = plugin;
    }

    protected void sendResponse(HttpExchange req, String msg) {
        sendResponse(req, 200, msg);
    }

    protected void sendResponse(HttpExchange req, HashMap<String, Object> jsonObj) {
        jsonObj.put("code", 200);
        jsonObj.put("error", "");
        sendResponse(req, new Gson().toJson(jsonObj));
    }

    protected void sendResponse(HttpExchange req, int code) {
        HashMap<String, Object> res = new HashMap<>();
        res.put("code", code);
        switch(code) {
            case 400 -> res.put("error", "Bad request");
            case 401 -> res.put("error", "Unauthorized");
            case 403 -> res.put("error", "Forbidden");
            case 404 -> res.put("error", "Not found");
            case 500 -> res.put("error", "Server internal error");
        }
        sendResponse(req, code, new Gson().toJson(res));
    }

    protected void sendResponse(HttpExchange req, int code, String msg) {
        Headers resHeaders = req.getResponseHeaders();
        resHeaders.add("Access-Control-Allow-Headers", "X-Credential-Token");
        resHeaders.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        resHeaders.add("Access-Control-Allow-Origin", "*");
        resHeaders.add("X-Powered-By", "OPanel");

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

    protected void sendContentResponse(HttpExchange req, byte[] bytes, String contentType) {
        Headers resHeaders = req.getResponseHeaders();
        resHeaders.add("X-Powered-By", "OPanel");

        try {
            req.getResponseHeaders().set("Content-Type", contentType);
            req.sendResponseHeaders(200, bytes.length);
            try(OutputStream os = req.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            sendResponse(req, 500);
            e.printStackTrace();
        }
    }

    protected <T> T getRequestBody(HttpExchange req, Class<T> type) throws IOException {
        final String reqBodyJson = new String(req.getRequestBody().readAllBytes());
        Gson gson = new Gson();
        return gson.fromJson(reqBodyJson, type);
    }

    protected boolean authCookie(HttpExchange req) {
        Headers headers = req.getRequestHeaders();
        String token = headers.getFirst("X-Credential-Token"); // hashed 2
        if(token == null) return false;

        final String hashedRealKey = Utils.md5(Utils.md5(plugin.getConfig().accessKey)); // hashed 2
        return token.equals(hashedRealKey);
    }
}
