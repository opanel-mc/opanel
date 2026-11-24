package net.opanel.controller.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import net.opanel.OPanel;
import net.opanel.config.OPanelConfiguration;
import net.opanel.controller.BaseController;
import net.opanel.web.AccessKeyHasher;
import net.opanel.web.JwtManager;
import net.opanel.web.LoginAttemptTracker;

import java.util.concurrent.TimeUnit;

public class AuthController extends BaseController {
    private final LoginAttemptTracker loginAttemptTracker = new LoginAttemptTracker();

    public AuthController(OPanel plugin) {
        super(plugin);
    }

    private boolean isCredentialInitialized(OPanelConfiguration config) {
        return config.accessKey != null
                && !config.accessKey.isBlank()
                && config.salt != null
                && !config.salt.isBlank();
    }

    public Handler login = ctx -> {
        ctx.header("Cache-Control", "no-store");
        final OPanelConfiguration config = plugin.getConfig();
        if(config.oidcEnabled) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Secret login is disabled when OIDC is enabled.");
            return;
        }
        if(!isCredentialInitialized(config)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Panel credential is not initialized.");
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
        if(reqBody == null || !AccessKeyHasher.isValidInputLength(reqBody.accessKey())) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Access key is invalid.");
            return;
        }

        final String reqIp = getIpAndCheck(ctx);
        if(reqIp == null) return;

        String activeHashedKey;
        try(AccessKeyHasher.RequestSlot slot = AccessKeyHasher.tryAcquireRequestSlot()) {
            if(slot == null) {
                ctx.header("Retry-After", "1");
                sendResponse(ctx, HttpStatus.TOO_MANY_REQUESTS, "Too many authentication requests are in progress.");
                return;
            }

            AccessKeyHasher.VerifyResult verifyResult = AccessKeyHasher.verify(reqBody.accessKey(), config.accessKey);
            if(verifyResult == AccessKeyHasher.VerifyResult.INVALID) {
                recordFailedLogin(ctx, reqIp);
                return;
            }

            loginAttemptTracker.recordSuccess(reqIp);

            activeHashedKey = config.accessKey;
            if(verifyResult == AccessKeyHasher.VerifyResult.VALID_NEEDS_UPGRADE) {
                activeHashedKey = AccessKeyHasher.hash(reqBody.accessKey());
                config.accessKey = activeHashedKey;
                plugin.setConfig(config);
            }
        }

        String token = JwtManager.generateToken(activeHashedKey, config.salt);
        // Context.cookie() provided by Javalin called List.removeFirst() method.
        // But the method was introduced in Java 21, so if OPanel is running under
        // Java versions lower than 21, this method will throw a NoSuchMethodError.
        //
        // Just simply catch it and do nothing.
        try {
            ctx.cookie(JwtManager.createCookie("token", token, (int) TimeUnit.DAYS.toSeconds(1), config.cookieSecure));
        } catch (NoSuchMethodError e) {
            //
        }
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler checkAuth = ctx -> {
        String token = ctx.cookie("token"); // jws
        final String hashedRealKey = plugin.getConfig().accessKey;
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
        String token = ctx.cookie("token");
        if(token != null) {
            OPanelConfiguration config = plugin.getConfig();
            JwtManager.revokeToken(token, config.accessKey, config.salt);
        }
        ctx.removeCookie("token");
        sendResponse(ctx, HttpStatus.OK);
    };

    private record RequestBodyType(String accessKey) {}

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
