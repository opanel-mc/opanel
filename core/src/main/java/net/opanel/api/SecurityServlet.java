package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.config.OPanelConfiguration;
import net.opanel.utils.Utils;
import net.opanel.web.BaseServlet;

import java.io.IOException;
import java.util.HashMap;

public class SecurityServlet extends BaseServlet {
    public static final String route = "/api/security";

    public SecurityServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        RequestBodyType reqBody = getRequestBody(req, RequestBodyType.class);
        if(reqBody.currentKey == null || reqBody.newKey == null) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final String currentKey = reqBody.currentKey; // hashed 1
        final String newKey = reqBody.newKey; // hashed 1
        final String realKey = plugin.getConfig().accessKey; // hashed 2

        if(!Utils.md5(currentKey).equals(realKey)) {
            sendResponse(res, HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Save new access key
        OPanelConfiguration config = plugin.getConfig();
        config.accessKey = Utils.md5(newKey);
        plugin.setConfig(config);

        // Send new token
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("token", Utils.md5(config.salt + config.accessKey));
        sendResponse(res, obj);
    }

    private class RequestBodyType {
        String currentKey;
        String newKey;
    }
}
