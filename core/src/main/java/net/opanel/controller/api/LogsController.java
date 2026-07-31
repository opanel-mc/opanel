package net.opanel.controller.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.logger.Loggable;
import net.opanel.controller.BaseController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.HashMap;

public class LogsController extends BaseController {
    private static final URI MCLOGS_API_URI = URI.create("https://api.mclo.gs/1/log");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    public Handler uploadLogToMclogs = ctx -> {
        final Loggable logger = plugin.logger;
        final String fileName = ctx.pathParam("fileName");
        final String content;
        try {
            content = logger.getLogContent(fileName);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the specified log file.");
            return;
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file extension.");
            return;
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        JsonArray metadata = new JsonArray();
        metadata.add(createMclogsMetadata(
                "server_software",
                server.getServerType().getName(),
                "Server Software"
        ));
        metadata.add(createMclogsMetadata(
                "mc_version",
                server.getVersion(),
                "Minecraft Version"
        ));
        metadata.add(createMclogsMetadata(
                "opanel_version",
                OPanel.VERSION,
                "OPanel Version"
        ));

        JsonObject payload = new JsonObject();
        payload.addProperty("content", content);
        payload.addProperty("source", "OPanel");
        payload.add("metadata", metadata);

        HttpRequest request = HttpRequest.newBuilder(MCLOGS_API_URI)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        final HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, "The request to mclo.gs was interrupted.");
            return;
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, "Failed to connect to mclo.gs.");
            return;
        }

        final JsonObject responseBody;
        final boolean uploadSucceeded;
        final String uploadError;
        final String logUrl;
        try {
            responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
            uploadSucceeded = (
                    responseBody.has("success")
                    && responseBody.get("success").getAsBoolean()
            );
            uploadError = (
                    responseBody.has("error") && !responseBody.get("error").isJsonNull()
                    ? responseBody.get("error").getAsString()
                    : null
            );
            logUrl = (
                    responseBody.has("url") && !responseBody.get("url").isJsonNull()
                    ? responseBody.get("url").getAsString()
                    : null
            );
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, "mclo.gs returned an invalid response.");
            return;
        }

        boolean success = (
                response.statusCode() >= 200
                && response.statusCode() < 300
                && uploadSucceeded
        );
        if(!success) {
            sendResponse(
                    ctx,
                    HttpStatus.BAD_GATEWAY,
                    uploadError != null
                    ? uploadError
                    : "mclo.gs rejected the log upload."
            );
            return;
        }

        if(logUrl == null) {
            sendResponse(ctx, HttpStatus.BAD_GATEWAY, "mclo.gs did not return a log URL.");
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("url", logUrl);
        sendResponse(ctx, obj);
    };

    private static JsonObject createMclogsMetadata(String key, String value, String label) {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("key", key);
        metadata.addProperty("value", value);
        metadata.addProperty("label", label);
        return metadata;
    }
}
