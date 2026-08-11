package net.opanel.controller.api;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.extension.ExtensionManager;
import net.opanel.extension.ExtensionMetadata;
import net.opanel.extension.LoadedExtension;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtensionsController extends BaseController {
    private static final String DISABLED_SUFFIX = ".disabled";

    private final DownloadController downloadController = getControllerInstance(DownloadController.class);

    public ExtensionsController(OPanel plugin) {
        super(plugin);
    }

    public Handler getExtensions = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        List<HashMap<String, Object>> extensions = new ArrayList<>();

        try(Stream<Path> paths = Files.list(OPanel.EXTENSIONS_DIR_PATH)) {
            List<Path> extensionPaths = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> isValidExtensionFileName(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());

            for(Path extensionPath : extensionPaths) {
                String fileName = extensionPath.getFileName().toString();
                ExtensionMetadata metadata;
                try {
                    metadata = ExtensionManager.readMetadata(extensionPath);
                } catch (Exception e) {
                    continue;
                }

                LoadedExtension loadedExtension = findLoadedExtension(extensionPath, metadata);
                HashMap<String, Object> extensionInfo = new HashMap<>();
                extensionInfo.put("fileName", Utils.stringToBase64(fileName));
                extensionInfo.put("extId", metadata.extId);
                extensionInfo.put("name", metadata.name);
                extensionInfo.put("version", metadata.version);
                extensionInfo.put("description", Utils.stringToBase64(metadata.description));
                extensionInfo.put("author", metadata.author);
                extensionInfo.put("size", Files.size(extensionPath));
                extensionInfo.put("enabled", !fileName.endsWith(DISABLED_SUFFIX));
                extensionInfo.put("hasWebIndex", loadedExtension != null && loadedExtension.hasResource("web/index.html"));
                extensions.add(extensionInfo);
            }
        } catch (IOException e) {
            plugin.logger.error("Failed to scan extensions: " + e.getMessage());
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        obj.put("extensions", extensions);
        obj.put("folderPath", OPanel.EXTENSIONS_DIR_PATH.toAbsolutePath().toString());
        sendResponse(ctx, obj);
    };

    public Handler uploadExtension = ctx -> {
        try {
            UploadedFile file = ctx.uploadedFile("file");
            if(file == null || file.size() <= 0) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "File is missing.");
                return;
            }

            String fileName = file.filename();
            if(!Utils.isSafeFileName(fileName)) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
                return;
            }
            if(!fileName.endsWith(".jar")) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Extension file should be a .jar file.");
                return;
            }

            Path targetPath = OPanel.EXTENSIONS_DIR_PATH.resolve(fileName);
            if(Files.exists(targetPath) || Files.exists(OPanel.EXTENSIONS_DIR_PATH.resolve(fileName + DISABLED_SUFFIX))) {
                sendResponse(ctx, HttpStatus.CONFLICT, "Extension file already exists.");
                return;
            }

            try(InputStream inputStream = file.content()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            plugin.getExtensionManager().loadExtension(targetPath);
            sendResponse(ctx, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler toggleExtension = ctx -> {
        String fileName = ctx.pathParam("fileName");
        String enabled = ctx.queryParam("enabled");
        if(!isValidExtensionFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }
        if(enabled == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Enabled status is missing.");
            return;
        }

        Path originalPath = OPanel.EXTENSIONS_DIR_PATH.resolve(fileName);
        if(!Files.isRegularFile(originalPath)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the extension.");
            return;
        }

        boolean isDisabled = fileName.endsWith(DISABLED_SUFFIX);
        ExtensionManager extensionManager = plugin.getExtensionManager();
        try {
            if(isDisabled && enabled.equals("1")) {
                Path enabledPath = OPanel.EXTENSIONS_DIR_PATH.resolve(removeDisabledSuffix(fileName));
                Files.move(originalPath, enabledPath);
                extensionManager.loadExtension(enabledPath);
            } else if(!isDisabled && !enabled.equals("1")) {
                LoadedExtension loadedExtension = findLoadedExtension(originalPath);
                if(loadedExtension != null) extensionManager.unloadExtension(loadedExtension);

                try {
                    Files.move(originalPath, OPanel.EXTENSIONS_DIR_PATH.resolve(fileName + DISABLED_SUFFIX));
                } catch (IOException e) {
                    if(loadedExtension != null) extensionManager.loadExtension(originalPath);
                    throw e;
                }
            }
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the extension.");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler deleteExtension = ctx -> {
        String fileName = ctx.pathParam("fileName");
        if(!isValidExtensionFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        Path filePath = findExtensionPath(fileName);
        if(filePath == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the extension.");
            return;
        }

        LoadedExtension loadedExtension = findLoadedExtension(filePath);
        try {
            if(loadedExtension != null) plugin.getExtensionManager().unloadExtension(loadedExtension);
            Files.delete(filePath);
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the extension.");
        } catch (Exception e) {
            if(loadedExtension != null && Files.exists(filePath)) {
                plugin.getExtensionManager().loadExtension(filePath);
            }
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler downloadExtension = ctx -> {
        String fileName = ctx.pathParam("fileName");
        if(!isValidExtensionFileName(fileName)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal file name.");
            return;
        }

        Path filePath = findExtensionPath(fileName);
        if(filePath == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the extension.");
            return;
        }

        String downloadId = downloadController.registerPath(filePath);
        ctx.redirect("/file/" + downloadId + "/" + removeDisabledSuffix(fileName));
    };

    public Handler getExtensionResource = ctx -> {
        String extensionId = ctx.pathParam("extId");
        String resourcePath = ctx.pathParamMap().containsKey("resource") ? ctx.pathParam("resource") : "";
        String normalizedPath = resourcePath.isEmpty() ? "index.html" : Utils.normalizePath(resourcePath);
        if(normalizedPath == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid extension resource path.");
            return;
        }

        ExtensionManager extensionManager = plugin.getExtensionManager();
        if(!extensionManager.hasExtension(extensionId)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension not found.");
            return;
        }
        if(!extensionManager.hasWebIndex(extensionId)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension page not found.");
            return;
        }

        try {
            String servedPath = resourcePath.endsWith("/") ? normalizedPath + "index.html" : normalizedPath;
            InputStream resource = extensionManager.openWebResource(extensionId, servedPath);
            if(resource == null && !resourcePath.endsWith("/")) {
                servedPath += "/index.html";
                resource = extensionManager.openWebResource(extensionId, servedPath);
            }
            if(resource == null) {
                sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension resource not found.");
                return;
            }

            ctx.status(HttpStatus.OK);
            ctx.writeSeekableStream(resource, getContentType(servedPath).toString());
        } catch (IOException e) {
            plugin.logger.error("Failed to read extension resource '" + extensionId + "/" + normalizedPath + "': " + e.getMessage());
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read extension resource.");
        }
    };

    private static ContentType getContentType(String resourcePath) {
        int extensionStart = resourcePath.lastIndexOf('.') + 1;
        String extension = extensionStart == 0 ? "" : resourcePath.substring(extensionStart);
        ContentType contentType = ContentType.getContentTypeByExtension(extension);
        return contentType == null ? ContentType.APPLICATION_OCTET_STREAM : contentType;
    }

    private LoadedExtension findLoadedExtension(Path extensionPath) {
        try {
            return findLoadedExtension(extensionPath, ExtensionManager.readMetadata(extensionPath));
        } catch (Exception e) {
            return null;
        }
    }

    private LoadedExtension findLoadedExtension(Path extensionPath, ExtensionMetadata metadata) {
        if(metadata == null || metadata.extId == null) return null;

        LoadedExtension extension = plugin.getExtensionManager().getExtension(metadata.extId);
        if(extension == null) return null;

        Path normalizedJarPath = extension.sourceJar.toAbsolutePath().normalize();
        Path normalizedExtensionPath = extensionPath.toAbsolutePath().normalize();
        return normalizedJarPath.equals(normalizedExtensionPath) ? extension : null;
    }

    private Path findExtensionPath(String fileName) {
        Path filePath = OPanel.EXTENSIONS_DIR_PATH.resolve(fileName);
        if(Files.isRegularFile(filePath)) return filePath;

        if(!fileName.endsWith(DISABLED_SUFFIX)) {
            Path disabledPath = OPanel.EXTENSIONS_DIR_PATH.resolve(fileName + DISABLED_SUFFIX);
            if(Files.isRegularFile(disabledPath)) return disabledPath;
        }
        return null;
    }

    private boolean isValidExtensionFileName(String fileName) {
        return Utils.isSafeFileName(fileName)
                && (fileName.endsWith(".jar") || fileName.endsWith(".jar" + DISABLED_SUFFIX));
    }

    private String removeDisabledSuffix(String fileName) {
        return fileName.endsWith(DISABLED_SUFFIX)
                ? fileName.substring(0, fileName.length() - DISABLED_SUFFIX.length())
                : fileName;
    }
}
