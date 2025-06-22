package net.opanel.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.opanel.OPanel;
import net.opanel.utils.ServerHandler;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.util.HashMap;

public class AuthHandler extends ServerHandler {
    public static final String route = "/api/auth";

    public AuthHandler(OPanel plugin) {
        super(plugin);
    }

    @Override
    public void handle(HttpExchange req) throws IOException {
        RequestBodyType reqBody = getRequestBody(req, RequestBodyType.class);
        if(reqBody.accessKey == null) {
            sendResponse(req, 400);
            return;
        }

        final String submittedKey = reqBody.accessKey; // hashed 1
        final String realKey = plugin.getConfig().accessKey; // hashed 0
        final String hashedSubmittedKey = Utils.md5(submittedKey); // hashed 2
        final String hashedRealKey = Utils.md5(Utils.md5(realKey)); // hashed 2

        if(hashedSubmittedKey.equals(hashedRealKey)) {
            HashMap<String, Object> res = new HashMap<>();
            res.put("token", hashedRealKey); // hashed 2
            sendResponse(req, res);
        } else {
            sendResponse(req, 401);
        }
    }

    private class RequestBodyType {
        String accessKey;
    }
}
