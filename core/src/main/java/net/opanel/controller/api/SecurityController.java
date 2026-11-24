package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.config.OPanelConfiguration;
import net.opanel.controller.BaseController;
import net.opanel.web.AccessKeyHasher;
import net.opanel.web.JwtManager;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class SecurityController extends BaseController {
    public SecurityController(OPanel plugin) {
        super(plugin);
    }

    public Handler updateAccessKey = ctx -> {
        RequestBodyType reqBody;
        try {
            if(ctx.body().isBlank()) throw new IllegalArgumentException();
            reqBody = ctx.bodyAsClass(RequestBodyType.class);
        } catch(Exception e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid request body.");
            return;
        }
        if(
            reqBody == null
            || !AccessKeyHasher.isValidInputLength(reqBody.currentKey())
            || !AccessKeyHasher.isValidNewKeyLength(reqBody.newKey())
        ) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid request body.");
            return;
        }

        OPanelConfiguration config = plugin.getConfig();
        try(AccessKeyHasher.RequestSlot slot = AccessKeyHasher.tryAcquireRequestSlot()) {
            if(slot == null) {
                ctx.header("Retry-After", "1");
                sendResponse(ctx, HttpStatus.TOO_MANY_REQUESTS, "Too many authentication requests are in progress.");
                return;
            }

            if(AccessKeyHasher.verify(reqBody.currentKey(), config.accessKey) == AccessKeyHasher.VerifyResult.INVALID) {
                sendResponse(ctx, HttpStatus.FORBIDDEN, "Access key mismatch.");
                return;
            }

            // Save new access key
            JwtManager.revokeAllTokens();
            config.accessKey = AccessKeyHasher.hash(reqBody.newKey());
            plugin.setConfig(config);
        }

        String token = JwtManager.generateToken(config.accessKey, config.salt);
        try {
            ctx.cookie(JwtManager.createCookie("token", token, (int) TimeUnit.DAYS.toSeconds(1), config.cookieSecure));
        } catch (NoSuchMethodError e) {
            //
        }
        sendResponse(ctx, new HashMap<>());
    };

    private record RequestBodyType(String currentKey, String newKey) {}
}
