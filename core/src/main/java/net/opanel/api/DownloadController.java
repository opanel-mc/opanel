package net.opanel.api;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.utils.Callback;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadController extends BaseController {
    private static final int idLength = 16;
    private final ConcurrentHashMap<String, Path> pathMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, byte[]> contentMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Callback> callbackMap = new ConcurrentHashMap<>();

    public DownloadController(OPanel plugin) {
        super(plugin);
    }

    public Handler downloadFile = ctx -> {
        final String id = ctx.pathParam("id");
        if(!pathMap.containsKey(id) && !contentMap.containsKey(id)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "File not found.");
            return;
        }

        final String fileName = ctx.pathParam("fileName");

        if(pathMap.containsKey(id)) {
            sendContent(ctx, Utils.readFile(pathMap.get(id)), ContentType.APPLICATION_OCTET_STREAM, fileName);
            if(callbackMap.containsKey(id)) {
                Callback cb = callbackMap.get(id);
                cb.action();
                callbackMap.remove(id);
            }
            pathMap.remove(id);
        } else {
            sendContent(ctx, contentMap.get(id), ContentType.APPLICATION_OCTET_STREAM, fileName);
            contentMap.remove(id);
        }
    };

    public String registerPath(Path path) {
        return registerPath(path, () -> {});
    }

    public String registerPath(Path path, Callback afterDownloading) {
        String id = generateUniqueId();
        pathMap.put(id, path);
        callbackMap.put(id, afterDownloading);
        return id;
    }

    public String registerContent(String content) {
        return registerContent(content.getBytes(StandardCharsets.UTF_8));
    }

    public String registerContent(byte[] bytes) {
        String id = generateUniqueId();
        contentMap.put(id, bytes);
        return id;
    }

    private String generateUniqueId() {
        String id = Utils.generateRandomCharSequence(idLength, false);
        while(pathMap.containsKey(id) || contentMap.containsKey(id)) {
            id = Utils.generateRandomCharSequence(idLength, false);
        }
        return id;
    }
}
