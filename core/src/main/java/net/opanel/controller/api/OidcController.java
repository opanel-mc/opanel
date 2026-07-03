package net.opanel.controller.api;

import com.nimbusds.jwt.JWTClaimsSet;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.config.OidcConfiguration;
import net.opanel.controller.BaseController;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.web.JwtManager;
import net.opanel.web.OidcManager;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class OidcController extends BaseController {
    private OidcManager oidcManager;
    private boolean initialized = false;

    private static final int MAX_VERIFY_TRIES = 5;
    private static final long BANNED_PERIOD = 10 * 60 * 1000; // 10 min
    private final ConcurrentHashMap<String, Integer> failedVerifyRecords = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> temporaryBannedRecords = new ConcurrentHashMap<>();

    public OidcController(OPanel plugin, OidcManager oidcManager) {
        super(plugin);
        this.oidcManager = oidcManager;
    }

    private void ensureInitialized() throws Exception {
        if(!plugin.getConfig().oidcEnabled) return;

        if(initialized && oidcManager.isDiscovered() && !oidcManager.needDiscovery()) return;

        oidcManager.discover(plugin.getConfig().oidcDiscoveryUrl, plugin.getConfig().oidcClientId);
        initialized = true;
    }

    public Handler login = ctx -> {
        if(!plugin.getConfig().oidcEnabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "OIDC is not enabled.");
            return;
        }

        try {
            ensureInitialized();
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to discover OIDC provider: " + e.getMessage());
            return;
        }

        try {
            String redirectUri = buildRedirectUri(ctx);
            String authUrl = oidcManager.buildAuthorizationUrl(plugin.getConfig().oidcClientId, redirectUri);
            ctx.redirect(authUrl, HttpStatus.FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build OIDC authorization URL: " + e.getMessage());
        }
    };

    public Handler callback = ctx -> {
        if(!plugin.getConfig().oidcEnabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "OIDC is not enabled.");
            return;
        }

        try {
            ensureInitialized();
        } catch (Exception e) {
            e.printStackTrace();
            ctx.redirect("/login?oidc-error=true", HttpStatus.FOUND);
            return;
        }

        String callbackUrl = ctx.fullUrl();
        String redirectUri = buildRedirectUri(ctx);

        JWTClaimsSet claims;
        try {
            claims = oidcManager.handleCallback(callbackUrl, plugin.getConfig().oidcClientId, plugin.getConfig().oidcClientSecret, redirectUri);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.redirect("/login?oidc-error=true", HttpStatus.FOUND);
            return;
        }

        String userId = claims.getSubject();
        if(userId == null || userId.isEmpty()) {
            ctx.redirect("/login?oidc-error=true", HttpStatus.FOUND);
            return;
        }

        OidcConfiguration oidcConfig = Storage.get().getStoredData(StorageKey.OIDC_CONFIG);
        if(oidcConfig != null && oidcConfig.allowedUserIds != null && oidcConfig.allowedUserIds.contains(userId)) {
            String token = JwtManager.generateToken(plugin.getConfig().accessKey, plugin.getConfig().salt);
            try {
                ctx.cookie(JwtManager.createCookie("token", token, (int) TimeUnit.DAYS.toSeconds(1), plugin.getConfig().cookieSecure));
            } catch (NoSuchMethodError e) {
                // Java < 21 compatibility
            }
            ctx.redirect("/panel/dashboard", HttpStatus.FOUND);
        } else {
            try {
                ctx.cookie(JwtManager.createCookie("oidc-pending-user", userId, 600, plugin.getConfig().cookieSecure));
            } catch (NoSuchMethodError e) {
                // Java < 21 compatibility
            }
            ctx.redirect("/login?oidc-bind=true", HttpStatus.FOUND);
        }
    };

    public Handler bindNewUser = ctx -> {
        if(!plugin.getConfig().oidcEnabled) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "OIDC is not enabled.");
            return;
        }

        String reqIp = getClientIp(ctx);
        if(reqIp == null || reqIp.isBlank()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Cannot determine client IP address.");
            return;
        }
        if(checkTemporaryBan(reqIp)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "The IP is banned temporarily.");
            return;
        }

        String pendingUserId = ctx.cookie("oidc-pending-user");
        if(pendingUserId == null || pendingUserId.isEmpty()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "No pending OIDC user. Please try logging in again.");
            return;
        }

        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if(reqBody == null || reqBody.accessKey == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Access key is missing.");
            return;
        }

        final String hashedAccessKey = plugin.getConfig().accessKey;
        if(!reqBody.accessKey.equals(hashedAccessKey)) {
            int current = failedVerifyRecords.merge(reqIp, 1, Integer::sum);
            if(current >= MAX_VERIFY_TRIES) {
                temporaryBannedRecords.put(reqIp, System.currentTimeMillis() + BANNED_PERIOD);
                failedVerifyRecords.remove(reqIp);
            }
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Access key mismatch.");
            return;
        }

        failedVerifyRecords.remove(reqIp);

        OidcConfiguration oidcConfig = Storage.get().getStoredData(StorageKey.OIDC_CONFIG);
        if(oidcConfig == null) {
            oidcConfig = new OidcConfiguration();
        }
        if(oidcConfig.allowedUserIds == null) {
            oidcConfig.allowedUserIds = new java.util.ArrayList<>();
        }
        if(!oidcConfig.allowedUserIds.contains(pendingUserId)) {
            oidcConfig.allowedUserIds.add(pendingUserId);
            Storage.get().setStoredData(StorageKey.OIDC_CONFIG, oidcConfig);
        }

        String token = JwtManager.generateToken(hashedAccessKey, plugin.getConfig().salt);
        try {
            ctx.cookie(JwtManager.createCookie("token", token, (int) TimeUnit.DAYS.toSeconds(1), plugin.getConfig().cookieSecure));
        } catch (NoSuchMethodError e) {
            // Java < 21 compatibility
        }
        ctx.removeCookie("oidc-pending-user");

        sendResponse(ctx, new HashMap<>());
    };

    public Handler getConfig = ctx -> {
        HashMap<String, Object> res = new HashMap<>();
        if(plugin.getConfig().oidcEnabled) {
            res.put("enabled", true);
            res.put("displayName", plugin.getConfig().oidcDisplayName);
        } else {
            res.put("enabled", false);
        }
        sendResponse(ctx, res);
    };

    public Handler getAllowedUsers = ctx -> {
        OidcConfiguration oidcConfig = Storage.get().getStoredData(StorageKey.OIDC_CONFIG);
        HashMap<String, Object> res = new HashMap<>();
        res.put("allowedUserIds", oidcConfig != null && oidcConfig.allowedUserIds != null ? oidcConfig.allowedUserIds : new java.util.ArrayList<>());
        sendResponse(ctx, res);
    };

    public Handler addAllowedUser = ctx -> {
        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if(reqBody == null || reqBody.userId == null || reqBody.userId.isBlank()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "User ID is missing.");
            return;
        }

        OidcConfiguration oidcConfig = Storage.get().getStoredData(StorageKey.OIDC_CONFIG);
        if(oidcConfig == null) {
            oidcConfig = new OidcConfiguration();
        }
        if(oidcConfig.allowedUserIds == null) {
            oidcConfig.allowedUserIds = new java.util.ArrayList<>();
        }
        if(!oidcConfig.allowedUserIds.contains(reqBody.userId)) {
            oidcConfig.allowedUserIds.add(reqBody.userId);
            Storage.get().setStoredData(StorageKey.OIDC_CONFIG, oidcConfig);
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("allowedUserIds", oidcConfig.allowedUserIds);
        sendResponse(ctx, res);
    };

    public Handler removeAllowedUser = ctx -> {
        RequestBodyType reqBody = ctx.bodyAsClass(RequestBodyType.class);
        if(reqBody == null || reqBody.userId == null || reqBody.userId.isBlank()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "User ID is missing.");
            return;
        }

        OidcConfiguration oidcConfig = Storage.get().getStoredData(StorageKey.OIDC_CONFIG);
        if(oidcConfig == null || oidcConfig.allowedUserIds == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "No allowed users.");
            return;
        }

        oidcConfig.allowedUserIds.remove(reqBody.userId);
        Storage.get().setStoredData(StorageKey.OIDC_CONFIG, oidcConfig);

        HashMap<String, Object> res = new HashMap<>();
        res.put("allowedUserIds", oidcConfig.allowedUserIds);
        sendResponse(ctx, res);
    };

    private String buildRedirectUri(io.javalin.http.Context ctx) {
        String scheme = ctx.scheme();
        if(plugin.getConfig().proxyHeaders) {
            String forwardedProto = ctx.header("X-Forwarded-Proto");
            if(forwardedProto != null && !forwardedProto.isEmpty()) {
                scheme = forwardedProto;
            }
        }
        String host = ctx.host();
        return scheme + "://" + host + "/api/auth/oidc/callback";
    }

    private static class RequestBodyType {
        String accessKey;
        String userId;
    }

    private boolean checkTemporaryBan(String ip) {
        Long banUntil = temporaryBannedRecords.get(ip);
        if(banUntil == null) return false;
        if(System.currentTimeMillis() < banUntil) return true;
        temporaryBannedRecords.remove(ip, banUntil);
        return false;
    }
}