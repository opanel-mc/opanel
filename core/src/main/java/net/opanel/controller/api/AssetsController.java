package net.opanel.controller.api;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class AssetsController extends BaseController {
    private static final String DEFAULT_EXT = "png";
    private static final String[] availableImageExt = { "png", "jpg", "jpeg", "webp" };
    private static final HashMap<String, String> knownAssets = new HashMap<>();

    private final HashMap<String, Path> loadedAssets = new HashMap<>();

    public AssetsController(OPanel plugin) {
        super(plugin);

        knownAssets.put("login-banner", "default-login-banner.png");

        for(String name : knownAssets.keySet()) {
            loadAsset(name, knownAssets.get(name));
        }
    }

    private void loadAsset(String name, String defaultResource) {
        for(String extname : availableImageExt) {
            Path filePath = OPanel.OPANEL_DIR_PATH.resolve(name +"."+ extname);
            if(Files.exists(filePath)) {
                loadedAssets.put(name, filePath);
                return;
            }
        }

        try(InputStream is = OPanel.class.getClassLoader().getResourceAsStream(defaultResource)) {
            if(is == null) {
                throw new IOException("Resource "+ defaultResource +" not found");
            }

            Path filePath = OPanel.OPANEL_DIR_PATH.resolve(name +"."+ DEFAULT_EXT);
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            loadedAssets.put(name, filePath);
        } catch (IOException e) {
            plugin.logger.error("Failed to extract "+ defaultResource +" from java resources: "+ e.getMessage());
        }
    }

    public Handler getAsset = ctx -> {
        final String assetName = ctx.pathParam("name");
        if(!knownAssets.containsKey(assetName) || !loadedAssets.containsKey(assetName)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Asset not found.");
            return;
        }

        if(!Files.exists(loadedAssets.get(assetName))) {
            loadAsset(assetName, knownAssets.get(assetName));
        }

        Path assetPath = loadedAssets.get(assetName);
        ContentType type = ContentType.contentTypeByExtension(Utils.getFileExtension(assetPath));
        sendContent(ctx, assetPath, type == null ? ContentType.APPLICATION_OCTET_STREAM : type);
    };

    public Handler uploadAsset = ctx -> {
        final String assetName = ctx.pathParam("name");
        if(!knownAssets.containsKey(assetName)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Unknown asset.");
            return;
        }

        UploadedFile file = ctx.uploadedFile("file");
        if(file == null || file.size() <= 0) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "File is missing.");
            return;
        }

        final String fileName = file.filename();
        final String extname = Utils.getFileExtension(Path.of(fileName));
        if(!Utils.arrayHas(availableImageExt, extname)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Asset extension name should be one of png, jpg, jpeg and webp.");
            return;
        }

        if(loadedAssets.containsKey(assetName)) {
            Files.deleteIfExists(loadedAssets.get(assetName));
        }

        try(InputStream is = file.content()) {
            Path filePath = OPanel.OPANEL_DIR_PATH.resolve(assetName +"."+ extname);
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            loadedAssets.put(assetName, filePath);
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler resetAsset = ctx -> {
        final String assetName = ctx.pathParam("name");
        if(!knownAssets.containsKey(assetName) || !loadedAssets.containsKey(assetName)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Asset not found.");
            return;
        }

        Files.deleteIfExists(loadedAssets.get(assetName));
        loadAsset(assetName, knownAssets.get(assetName));
        sendResponse(ctx, HttpStatus.OK);
    };
}
