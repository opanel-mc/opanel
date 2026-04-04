package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import net.opanel.OPanel;
import net.opanel.utils.Utils;
import net.opanel.controller.BaseController;
import net.opanel.web.JwtManager;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuthController extends BaseController {
    private final ConcurrentHashMap<String, String> cramMap = new ConcurrentHashMap<>();

    private static final int maxTries = 5;
    private static final long bannedPeriod = 10 * 60 * 1000; // 10 min
    private final ConcurrentHashMap<String, Integer> failedRecords = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> temporaryBannedRecords = new ConcurrentHashMap<>(); // ms

    public AuthController(OPanel plugin) {
        super(plugin);
    }

    public Handler getCram = ctx -> {
        final String id = ctx.queryParam("id");
        if(id == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Id is missing.");
            return;
        }

        final String reqIp = getClientIp(ctx);
        if(reqIp.isBlank()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Cannot determine client IP address.");
            return;
        }
        if(System.currentTimeMillis() < temporaryBannedRecords.getOrDefault(reqIp, 0L)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The Ip is banned temporarily.");
            return;
        }
        if(failedRecords.getOrDefault(reqIp, 0) >= maxTries) {
            temporaryBannedRecords.put(reqIp, System.currentTimeMillis() + bannedPeriod);
            failedRecords.put(reqIp, 0);
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The Ip is banned temporarily.");
            return;
        }
        if(temporaryBannedRecords.containsKey(reqIp) && System.currentTimeMillis() >= temporaryBannedRecords.get(reqIp)) {
            temporaryBannedRecords.remove(reqIp);
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

        final String reqIp = getClientIp(ctx);
        final String challengeResult = reqBody.result; // hashed 3
        final String storedRealKey = plugin.getConfig().accessKey; // hashed 2
        final String realResult = Utils.md5(storedRealKey + cramMap.get(reqBody.id)); // hashed 3
        cramMap.remove(reqBody.id);

        if(challengeResult.equals(realResult)) {
            failedRecords.remove(reqIp);

            String token = JwtManager.generateToken(storedRealKey, plugin.getConfig().salt);
            ctx.cookie(JwtManager.createCookie("token", token, (int) TimeUnit.DAYS.toSeconds(1), plugin.getConfig().cookieSecure));
            sendResponse(ctx, HttpStatus.OK);
        } else {
            final int current = failedRecords.getOrDefault(reqIp, 0);
            failedRecords.put(reqIp, current + 1);
            if(current + 1 >= maxTries) {
                temporaryBannedRecords.put(reqIp, System.currentTimeMillis() + bannedPeriod);
                failedRecords.put(reqIp, 0);
            }

            plugin.logger.warn("A failed login request from "+ reqIp +" (Failed for "+ (current + 1) +" times)");
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
        }
    };

    public Handler checkAuth = ctx -> {
        String token = ctx.cookie("token"); // jws
        final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
        if(token == null) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED, "Token is missing.");
            return;
        }
        if(!JwtManager.verifyToken(token, hashedRealKey, plugin.getConfig().salt)) {
            ctx.removeCookie("token");
            sendResponse(ctx, HttpStatus.UNAUTHORIZED, "Token is invalid.");
            return;
        }
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler logout = ctx -> {
        ctx.removeCookie("token");
        sendResponse(ctx, HttpStatus.OK);
    };

    private static class RequestBodyType {
        String id;
        String result; // Challenge result
    }
}
