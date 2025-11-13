package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;
import net.opanel.web.JwtManager;

import java.util.HashMap;

public class AuthController extends BaseController {
    private final HashMap<String, String> cramMap = new HashMap<>();

    private static final int maxTries = 5;
    private static final long bannedPeriod = 10 * 60 * 1000; // 10 min
    private final HashMap<String, Integer> failedRecords = new HashMap<>();
    private final HashMap<String, Long> temporaryBannedRecords = new HashMap<>(); // ms

    public AuthController(OPanel plugin) {
        super(plugin);
    }

    public Handler getCram = ctx -> {
        final String id = ctx.queryParam("id");
        if(id == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Id is missing.");
            return;
        }

        final String remoteHost = ctx.host();
        if(System.currentTimeMillis() < temporaryBannedRecords.getOrDefault(remoteHost, 0L)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The Ip is banned temporarily.");
            return;
        }
        if(failedRecords.getOrDefault(remoteHost, 0) >= maxTries) {
            temporaryBannedRecords.put(remoteHost, System.currentTimeMillis() + bannedPeriod);
            failedRecords.put(remoteHost, 0);
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The Ip is banned temporarily.");
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

        HashMap<String, Object> res = new HashMap<>();
        res.put("cram", cramRandomHex);
        sendResponse(ctx, res);
    };

    public Handler validateCram = ctx -> {
        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if(reqBody.id == null || reqBody.result == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Id or result is missing.");
            return;
        }

        final String remoteHost = ctx.host();
        final String challengeResult = reqBody.result; // hashed 3
        final String storedRealKey = plugin.getConfig().accessKey; // hashed 2
        final String realResult = Utils.md5(storedRealKey + cramMap.get(reqBody.id)); // hashed 3
        cramMap.remove(reqBody.id);

        if(challengeResult.equals(realResult)) {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("token", JwtManager.generateToken(storedRealKey, plugin.getConfig().salt));
            failedRecords.remove(remoteHost);
            sendResponse(ctx, obj);
        } else {
            final int current = failedRecords.getOrDefault(remoteHost, 0);
            failedRecords.put(remoteHost, current + 1);
            if(current + 1 >= maxTries) {
                temporaryBannedRecords.put(remoteHost, System.currentTimeMillis() + bannedPeriod);
                failedRecords.put(remoteHost, 0);
            }

            plugin.logger.warn("A failed login request from "+ remoteHost +" (Failed for "+ (current + 1) +" times)");
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
        }
    };

    private static class RequestBodyType {
        String id;
        String result; // Challenge result
    }
}
