package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.utils.Utils;
import net.opanel.web.BaseServlet;
import net.opanel.web.JwtManager;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;

public class AuthServlet extends BaseServlet {
    public static final String route = "/api/auth";
    private final HashMap<String, String> cramMap = new HashMap<>();

    private static final int maxTries = 5;
    private static final long bannedPeriod = 10 * 60 * 1000; // 10 min
    private final HashMap<String, Integer> failedRecords = new HashMap<>();
    private final HashMap<String, Long> temporaryBannedRecords = new HashMap<>(); // ms

    public AuthServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        final String id = req.getParameter("id");
        if(id == null) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final String remoteHost = req.getRemoteHost();
        if(System.currentTimeMillis() < temporaryBannedRecords.getOrDefault(remoteHost, 0L)) {
            sendResponse(res, HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if(failedRecords.getOrDefault(remoteHost, 0) >= maxTries) {
            temporaryBannedRecords.put(remoteHost, System.currentTimeMillis() + bannedPeriod);
            failedRecords.put(remoteHost, 0);
            sendResponse(res, HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if(temporaryBannedRecords.containsKey(remoteHost) && System.currentTimeMillis() >= temporaryBannedRecords.get(remoteHost)) {
            temporaryBannedRecords.remove(remoteHost);
        }

        String cramRandomHex = Utils.generateRandomHex(16);
        while(cramMap.containsValue(cramRandomHex)) {
            cramRandomHex = Utils.generateRandomHex(16);
        }
        cramMap.put(id, cramRandomHex);

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("cram", cramRandomHex);
        sendResponse(res, obj);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        RequestBodyType reqBody = getRequestBody(req, RequestBodyType.class);
        if(reqBody.id == null || reqBody.result == null) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final String remoteHost = req.getRemoteHost();
        final int remotePort = req.getRemotePort();
        final String challengeResult = reqBody.result; // hashed 3
        final String storedRealKey = plugin.getConfig().accessKey; // hashed 2
        final String realResult = Utils.md5(storedRealKey + cramMap.get(reqBody.id)); // hashed 3
        cramMap.remove(reqBody.id);

        if(challengeResult.equals(realResult)) {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("token", JwtManager.generateToken(storedRealKey, plugin.getConfig().salt));
            failedRecords.remove(remoteHost);
            sendResponse(res, obj);
        } else {
            final int current = failedRecords.getOrDefault(remoteHost, 0);
            failedRecords.put(remoteHost, current + 1);
            if(current + 1 >= maxTries) {
                temporaryBannedRecords.put(remoteHost, System.currentTimeMillis() + bannedPeriod);
                failedRecords.put(remoteHost, 0);
            }

            plugin.logger.warn("A failed login request from "+ remoteHost +":"+ remotePort +" (Failed for "+ (current + 1) +" times)");
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private class RequestBodyType {
        String id;
        String result; // Challenge result
    }
}
