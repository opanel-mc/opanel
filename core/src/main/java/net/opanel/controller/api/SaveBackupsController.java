package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.backup.BackupRecord;
import net.opanel.backup.BackupService;
import net.opanel.controller.BaseController;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class SaveBackupsController extends BaseController {
    private BackupService backupService;

    public SaveBackupsController(OPanel plugin) {
        super(plugin);
        backupService = BackupService.get(plugin);
    }

    public Handler getBackups = ctx -> {
        String saveName = ctx.pathParam("saveName");
        List<BackupRecord> backups = backupService.getSaveBackups(saveName);

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("backups", backups);
        sendResponse(ctx, obj);
    };

    public Handler getBackup = ctx -> {
        String saveName = ctx.pathParam("saveName");
        String backupId = ctx.pathParam("backupId");

        try {
            BackupRecord backup = backupService.getBackup(saveName, backupId);
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("backup", backup);
            sendResponse(ctx, obj);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler createBackup = ctx -> {
        final String saveName = ctx.pathParam("saveName");

        String providerId = ctx.queryParam("provider");
        if((providerId == null || providerId.isBlank()) && !ctx.body().isBlank()) {
            CreateBackupRequestBodyType reqBody = ctx.bodyAsClass(CreateBackupRequestBodyType.class);
            providerId = reqBody.providerId;
        }

        try {
            BackupRecord record = backupService.createBackup(saveName, providerId);
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("backupId", record.id);
            obj.put("status", record.status);
            sendResponse(ctx, obj);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            sendResponse(ctx, HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler restoreBackup = ctx -> {
        final String saveName = ctx.pathParam("saveName");
        final String backupId = ctx.pathParam("backupId");

        String targetSaveName = null;
        if(!ctx.body().isBlank()) {
            RestoreBackupRequestBodyType reqBody = ctx.bodyAsClass(RestoreBackupRequestBodyType.class);
            targetSaveName = reqBody.targetSaveName;
        }

        try {
            String restoredSaveName = backupService.restoreBackup(saveName, backupId, targetSaveName);
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("restoredSaveName", restoredSaveName);
            sendResponse(ctx, obj);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            sendResponse(ctx, HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler deleteBackup = ctx -> {
        final String saveName = ctx.pathParam("saveName");
        final String backupId = ctx.pathParam("backupId");

        try {
            backupService.deleteBackup(saveName, backupId);
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    private static class CreateBackupRequestBodyType {
        String providerId;
    }

    private static class RestoreBackupRequestBodyType {
        String targetSaveName;
    }
}
