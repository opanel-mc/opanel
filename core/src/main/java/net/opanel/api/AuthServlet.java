package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.utils.Utils;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.util.HashMap;

public class AuthServlet extends BaseServlet {
    public static final String route = "/api/auth";

    public AuthServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        RequestBodyType reqBody = getRequestBody(req, RequestBodyType.class);
        if(reqBody.accessKey == null) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final String submittedKey = reqBody.accessKey; // hashed 1
        final String realKey = plugin.getConfig().accessKey; // hashed 0
        final String hashedSubmittedKey = Utils.md5(submittedKey); // hashed 2
        final String hashedRealKey = Utils.md5(Utils.md5(realKey)); // hashed 2

        if(hashedSubmittedKey.equals(hashedRealKey)) {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("token", hashedRealKey); // hashed 2
            sendResponse(res, obj);
        } else {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private class RequestBodyType {
        String accessKey;
    }
}
