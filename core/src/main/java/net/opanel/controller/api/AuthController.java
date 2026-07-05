package net.opanel.controller.api;

import io.javalin.http.Context;
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

    private boolean isOidcEnabled() {
        return plugin.getConfig().oidcEnabled;
    }

    public Handler getCram = ctx -> {
        if(isOidcEnabled()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Secret login is disabled when OIDC is enabled.");
            return;
        }

        final String id = ctx.queryParam("id");
        if(id == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Id is missing.");
            return;
        }

        final String reqIp = getIpAndCheck(ctx);
        if(reqIp == null) return;

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
        if(isOidcEnabled()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Secret login is disabled when OIDC is enabled.");
            return;
        }

        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if(reqBody.id == null || reqBody.result == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Id or result is missing.");
            return;
        }

        final String reqIp = getIpAndCheck(ctx);
        if(reqIp == null) return;

        final String challengeResult = reqBody.result; // hashed 3
        final String storedRealKey = plugin.getConfig().accessKey; // hashed 2
        final String realResult = Utils.md5(storedRealKey + cramMap.get(reqBody.id)); // hashed 3
        cramMap.remove(reqBody.id);

        if(challengeResult.equals(realResult)) {
            removeFailedRecord(reqIp);

            String token = JwtManager.generateToken(storedRealKey, plugin.getConfig().salt);
            // Context.cookie() provided by Javalin called List.removeFirst() method.
            // But the method was introduced in Java 21, so if OPanel is running under
            // Java versions lower than 21, this method will throw a NoSuchMethodError.
            //
            // Just simply catch it and do nothing.
            try {
                ctx.cookie(JwtManager.createCookie("token", token, (int) TimeUnit.DAYS.toSeconds(1), plugin.getConfig().cookieSecure));
            } catch (NoSuchMethodError e) {
                //
            }
            sendResponse(ctx, HttpStatus.OK);
        } else {
            final int current = incrementFailedCount(reqIp);
            if(current >= maxTries) {
                setTemporaryBan(reqIp);
            }

            plugin.logger.warn("A failed login request from "+ reqIp +" (Failed for "+ current +" times)");
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

    private String getIpAndCheck(Context ctx) {
        final String reqIp = getClientIp(ctx);
        if(reqIp == null || reqIp.isBlank()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Cannot determine client IP address.");
            return null;
        }
        if(checkTemporaryBan(reqIp)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The Ip is banned temporarily.");
            return null;
        }
        if(checkFailedAndBanIfExceeded(reqIp)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The Ip is banned temporarily.");
            return null;
        }
        return reqIp;
    }

    private int incrementFailedCount(String ip) {
        return failedRecords.merge(ip, 1, Integer::sum);
    }

    private int getFailedCount(String ip) {
        return failedRecords.getOrDefault(ip, 0);
    }

    private void removeFailedRecord(String ip) {
        failedRecords.remove(ip);
    }

    private boolean checkTemporaryBan(String ip) {
        long currentTime = System.currentTimeMillis();
        Long banUntil = temporaryBannedRecords.get(ip);

        if(banUntil == null) {
            return false;
        }

        if(currentTime < banUntil) {
            return true;
        }

        temporaryBannedRecords.remove(ip, banUntil);
        return false;
    }

    private void setTemporaryBan(String ip) {
        temporaryBannedRecords.put(ip, System.currentTimeMillis() + bannedPeriod);
        failedRecords.put(ip, 0);
    }

    private boolean checkFailedAndBanIfExceeded(String ip) {
        int currentCount = getFailedCount(ip);
        if(currentCount >= maxTries) {
            setTemporaryBan(ip);
            removeFailedRecord(ip);
            return true;
        }
        return false;
    }
}