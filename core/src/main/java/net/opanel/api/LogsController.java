package net.opanel.api;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.logger.Loggable;
import net.opanel.web.BaseController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;

public class LogsController extends BaseController {
    private final DownloadController downloadController = getControllerInstance(DownloadController.class);

    public LogsController(OPanel plugin) {
        super(plugin);
    }

    public Handler getLogFileList = ctx -> {
        final Loggable logger = plugin.logger;
        try {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("logs", logger.getLogFileList());
            sendResponse(ctx, obj);
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler getLogContent = ctx -> {
        final Loggable logger = plugin.logger;
        final String fileName = ctx.pathParam("fileName");
        try {
            sendContent(ctx, logger.getLogContent(fileName).getBytes(StandardCharsets.UTF_8), ContentType.TEXT_PLAIN);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the specified log file.");
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file extension.");
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler downloadLog = ctx -> {
        final Loggable logger = plugin.logger;
        final String fileName = ctx.pathParam("fileName");
        final String downloadedFileName = fileName.endsWith(".log.gz") ? fileName.replace(".log.gz", ".log") : fileName;
        final String downloadId = downloadController.registerContent(logger.getLogContent(fileName));
        ctx.redirect("/file/"+ downloadId +"/"+ downloadedFileName);
    };

    public Handler clearLogs = ctx -> {
        final Loggable logger = plugin.logger;
        try {
            for(String fileName : logger.getLogFileList()) {
                if(fileName.endsWith(".log.gz")) {
                    logger.deleteLog(fileName);
                }
            }
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the specified log file.");
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler deleteLog = ctx -> {
        final Loggable logger = plugin.logger;
        final String fileName = ctx.pathParam("fileName");
        if(fileName.endsWith(".log")) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "You cannot delete latest.log or debug.log.");
            return;
        }

        try {
            logger.deleteLog(fileName);
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the specified log file.");
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };
}
