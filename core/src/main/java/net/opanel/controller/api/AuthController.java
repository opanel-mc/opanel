package net.opanel.controller.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import net.opanel.OPanel;
import net.opanel.utils.Utils;
import net.opanel.controller.BaseController;
import net.opanel.web.CramChallengeStore;
import net.opanel.web.JwtManager;
import net.opanel.web.LoginAttemptTracker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AuthController extends BaseController {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern RESULT_PATTERN = Pattern.compile("[0-9a-f]{32}");

    private final CramChallengeStore cramChallengeStore = new CramChallengeStore();
    private final LoginAttemptTracker loginAttemptTracker = new LoginAttemptTracker();

    public AuthController(OPanel plugin) {
        super(plugin);
    }

    private boolean isOidcEnabled() {
        return plugin.getConfig().oidcEnabled;
    }

    public Handler getCram = ctx -> {
        ctx.header("Cache-Control", "no-store");
        if(isOidcEnabled()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Secret login is disabled when OIDC is enabled.");
            return;
        }

        final String id = ctx.queryParam("id");
        if(id == null || !ID_PATTERN.matcher(id).matches()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Id is invalid.");
            return;
        }

        final String reqIp = getIpAndCheck(ctx);
        if(reqIp == null) return;

        CramChallengeStore.CreateResult createResult = cramChallengeStore.create(reqIp, id);
        if(createResult.status() == CramChallengeStore.CreateStatus.DUPLICATE) {
            sendResponse(ctx, HttpStatus.CONFLICT, "Id is already in use.");
            return;
        }
        if(createResult.status() == CramChallengeStore.CreateStatus.CAPACITY_FULL) {
            ctx.header("Retry-After", Long.toString(createResult.retryAfterSeconds()));
            sendResponse(ctx, HttpStatus.TOO_MANY_REQUESTS, "Too many login challenges are active.");
            return;
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("cram", createResult.challenge());
        sendResponse(ctx, res);
    };

    public Handler validateCram = ctx -> {
        ctx.header("Cache-Control", "no-store");
        if(isOidcEnabled()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Secret login is disabled when OIDC is enabled.");
            return;
        }

        RequestBodyType reqBody;
        try {
            if(ctx.body().isBlank()) throw new IllegalArgumentException();
            reqBody = ctx.bodyAsClass(RequestBodyType.class);
        } catch(Exception e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Request body is invalid.");
            return;
        }
        if(reqBody == null
                || reqBody.id() == null
                || !ID_PATTERN.matcher(reqBody.id()).matches()
                || reqBody.result() == null
                || !RESULT_PATTERN.matcher(reqBody.result()).matches()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Id or result is invalid.");
            return;
        }

        final String reqIp = getIpAndCheck(ctx);
        if(reqIp == null) return;

        final String challenge = cramChallengeStore.consume(reqIp, reqBody.id());
        if(challenge == null) {
            recordFailedLogin(ctx, reqIp);
            return;
        }

        final String challengeResult = reqBody.result(); // hashed 3
        final String storedRealKey = plugin.getConfig().accessKey; // hashed 2
        final String realResult = Utils.md5(storedRealKey + challenge); // hashed 3

        if(MessageDigest.isEqual(
                challengeResult.getBytes(StandardCharsets.US_ASCII),
                realResult.getBytes(StandardCharsets.US_ASCII)
        )) {
            loginAttemptTracker.recordSuccess(reqIp);

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
            recordFailedLogin(ctx, reqIp);
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

    private record RequestBodyType(String id, String result) {}

    private String getIpAndCheck(Context ctx) {
        final String reqIp = getClientIp(ctx);
        if(reqIp == null || reqIp.isBlank()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Cannot determine client IP address.");
            return null;
        }
        LoginAttemptTracker.Result result = loginAttemptTracker.check(reqIp);
        if(result.status() != LoginAttemptTracker.Status.ALLOWED) {
            sendThrottleResponse(ctx, result);
            return null;
        }
        return reqIp;
    }

    private void recordFailedLogin(Context ctx, String reqIp) {
        LoginAttemptTracker.Result result = loginAttemptTracker.recordFailure(reqIp);
        if(result.status() != LoginAttemptTracker.Status.ALLOWED) {
            sendThrottleResponse(ctx, result);
            return;
        }

        plugin.logger.warn("A failed login request from "+ reqIp +" (Failed for "+ result.failedAttempts() +" times)");
        sendResponse(ctx, HttpStatus.UNAUTHORIZED);
    }

    private void sendThrottleResponse(Context ctx, LoginAttemptTracker.Result result) {
        if(result.status() == LoginAttemptTracker.Status.BANNED) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The Ip is banned temporarily.");
        } else {
            ctx.header("Retry-After", Long.toString(result.retryAfterSeconds()));
            sendResponse(ctx, HttpStatus.TOO_MANY_REQUESTS, "Too many login sources are being tracked.");
        }
    }
}
