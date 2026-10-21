package net.opanel.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Stream;

public class BackgroundsController extends BaseController {
    private static final Gson gson = new Gson();
    private static final String[] SUPPORTED_IMAGE_EXTS = { "png", "jpg", "jpeg", "webp" };

    public BackgroundsController(OPanel plugin) {
        super(plugin);
    }

    private boolean isImageFile(Path path) {
        String ext = Utils.getFileExtension(path).toLowerCase();
        for(String supported : SUPPORTED_IMAGE_EXTS) {
            if(supported.equals(ext)) return true;
        }
        return false;
    }

    public Handler listBackgrounds = ctx -> {
        JsonArray files = new JsonArray();
        Path backgroundsDir = OPanel.BACKGROUNDS_DIR_PATH;

        if(Files.exists(backgroundsDir)) {
            try(Stream<Path> stream = Files.list(backgroundsDir)) {
                stream.filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("name", path.getFileName().toString());
                        obj.addProperty("url", "/backgrounds/" + path.getFileName().toString());
                        try {
                            obj.addProperty("size", Files.size(path));
                        } catch(IOException e) {
                            obj.addProperty("size", 0);
                        }
                        files.add(obj);
                    });
            } catch(IOException e) {
                sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list backgrounds: " + e.getMessage());
                return;
            }
        }

        HashMap<String, Object> response = new HashMap<>();
        response.put("files", gson.toJsonTree(files));
        sendResponse(ctx, response);
    };

    public Handler getBackground = ctx -> {
        String filename = ctx.pathParam("filename");
        Path filePath = OPanel.BACKGROUNDS_DIR_PATH.resolve(filename).normalize();

        if(!filePath.startsWith(OPanel.BACKGROUNDS_DIR_PATH)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Access denied.");
            return;
        }

        if(!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Background not found.");
            return;
        }

        if(!isImageFile(filePath)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Unsupported file type.");
            return;
        }

        ContentType type = ContentType.contentTypeByExtension(Utils.getFileExtension(filePath));
        sendContent(ctx, filePath, type == null ? ContentType.APPLICATION_OCTET_STREAM : type);
    };

    public Handler uploadBackground = ctx -> {
        UploadedFile file = ctx.uploadedFile("file");
        if(file == null || file.size() <= 0) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "File is missing.");
            return;
        }

        String originalFilename = file.filename();
        String ext = Utils.getFileExtension(Path.of(originalFilename)).toLowerCase();
        boolean validExt = false;
        for(String supported : SUPPORTED_IMAGE_EXTS) {
            if(supported.equals(ext)) {
                validExt = true;
                break;
            }
        }
        if(!validExt) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Only png, jpg, jpeg and webp images are supported.");
            return;
        }

        String safeName = originalFilename.replaceAll("[\\\\/]", "_");

        try(InputStream is = file.content()) {
            Path destPath = OPanel.BACKGROUNDS_DIR_PATH.resolve(safeName);
            if(Files.exists(destPath)) {
                sendResponse(ctx, HttpStatus.CONFLICT, "A file with this name already exists.");
                return;
            }
            Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
            sendResponse(ctx, HttpStatus.OK);
        } catch(IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload: " + e.getMessage());
        }
    };

    public Handler deleteBackground = ctx -> {
        String filename = ctx.pathParam("filename");
        Path filePath = OPanel.BACKGROUNDS_DIR_PATH.resolve(filename).normalize();

        if(!filePath.startsWith(OPanel.BACKGROUNDS_DIR_PATH)) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Access denied.");
            return;
        }

        if(!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Background not found.");
            return;
        }

        try {
            Files.delete(filePath);
            sendResponse(ctx, HttpStatus.OK);
        } catch(IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete: " + e.getMessage());
        }
    };
}