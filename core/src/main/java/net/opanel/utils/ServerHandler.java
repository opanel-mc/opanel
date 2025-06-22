package net.opanel.utils;

import com.google.gson.Gson;
import com.sun.net.httpserver.*;
import net.opanel.OPanel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
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
        resHeaders.add("Access-Control-Allow-Headers","x-prototype-version,x-requested-with");
        resHeaders.add("Access-Control-Allow-Methods","GET,POST,PUT,DELETE");
        resHeaders.add("Access-Control-Allow-Origin","*");

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

    protected <T> T getRequestBody(HttpExchange req, Class<T> type) throws IOException {
        final String reqBodyJson = new String(req.getRequestBody().readAllBytes());
        Gson gson = new Gson();
        return gson.fromJson(reqBodyJson, type);
    }

    protected boolean authCookie(HttpExchange req) {
        Headers headers = req.getRequestHeaders();
        String cookieHeader = headers.getFirst("Cookie");
        if(cookieHeader == null || cookieHeader.isEmpty()) return false;

        List<HttpCookie> cookies = HttpCookie.parse(cookieHeader);
        if(cookies.isEmpty()) return false;

        for(HttpCookie cookie : cookies) {
            if(cookie.getName().equals("token")) {
                final String token = cookie.getValue(); // hashed 2
                final String hashedRealKey = Utils.md5(Utils.md5(plugin.getConfig().accessKey)); // hashed 2
                return token.equals(hashedRealKey);
            }
        }
        return false;
    }
}
