package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.backup.BackupProviderConfig;
import net.opanel.backup.BackupService;
import net.opanel.controller.BaseController;

import java.util.HashMap;
import java.util.NoSuchElementException;

public class BackupProvidersController extends BaseController {
    private BackupService backupService;

    public BackupProvidersController(OPanel plugin) {
        super(plugin);
        backupService = BackupService.get(plugin);
    }

    public Handler getProviders = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("providers", backupService.getProviders(true));
        sendResponse(ctx, obj);
    };

    public Handler createProvider = ctx -> {
        try {
            BackupProviderConfig reqBody = ctx.bodyAsClass(BackupProviderConfig.class);
            BackupProviderConfig provider = backupService.createProvider(reqBody);

            HashMap<String, Object> obj = new HashMap<>();
            obj.put("providerId", provider.id);
            sendResponse(ctx, obj);
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler editProvider = ctx -> {
        final String providerId = ctx.pathParam("id");

        try {
            BackupProviderConfig reqBody = ctx.bodyAsClass(BackupProviderConfig.class);
            backupService.updateProvider(providerId, reqBody);
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler deleteProvider = ctx -> {
        final String providerId = ctx.pathParam("id");

        try {
            backupService.deleteProvider(providerId);
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            sendResponse(ctx, HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler testProvider = ctx -> {
        final String providerId = ctx.pathParam("id");

        try {
            backupService.testProvider(providerId);
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };
}
