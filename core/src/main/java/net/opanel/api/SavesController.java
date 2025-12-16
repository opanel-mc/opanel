package net.opanel.api;

import io.javalin.http.*;
import net.opanel.OPanel;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.utils.Utils;
import net.opanel.utils.ZipUtility;
import net.opanel.web.BaseController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class SavesController extends BaseController {
    private final DownloadController downloadController = getControllerInstance(DownloadController.class);

    public SavesController(OPanel plugin) {
        super(plugin);
    }

    public Handler getSaves = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();

        List<HashMap<String, Object>> saves = new ArrayList<>();
        for(OPanelSave save : server.getSaves()) {
            HashMap<String, Object> saveInfo = new HashMap<>();
            saveInfo.put("name", save.getName());
            saveInfo.put("displayName", Utils.stringToBase64(save.getDisplayName()));
            saveInfo.put("path", save.getPath().toString());
            saveInfo.put("size", save.getSize());
            saveInfo.put("isRunning", save.isRunning());
            saveInfo.put("isCurrent", save.isCurrent());
            saveInfo.put("defaultGameMode", save.getDefaultGameMode().getName());
            saves.add(saveInfo);
        }
        obj.put("saves", saves);

        sendResponse(ctx, obj);
    };

    public Handler downloadSave = ctx -> {
        final String saveName = ctx.pathParam("saveName");
        OPanelSave save = server.getSave(saveName);
        if(save == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the specified save.");
            return;
        }

        // force saving world before making zip if the save is currently running on the server
        if(save.isRunning()) server.saveAll();

        Path savePath = save.getPath();
        Path zipPath = OPanel.TMP_DIR_PATH.resolve(UUID.randomUUID() +".zip");

        try {
            /*
             * Bukkit separates nether and the end dimension from the save folder,
             * so we need to put them together when processing the save files
             */
            if(server.getServerType().isBukkitSeries()) {
                Path netherDim = Paths.get("").resolve(saveName +"_nether/DIM-1");
                Path theEndDim = Paths.get("").resolve(saveName +"_the_end/DIM1");
                if(Files.exists(netherDim)) Utils.copyDirectoryRecursively(netherDim, savePath.resolve("DIM-1"));
                if(Files.exists(theEndDim)) Utils.copyDirectoryRecursively(theEndDim, savePath.resolve("DIM1"));
            }

            ZipUtility.zip(savePath, zipPath);

            final String downloadId = downloadController.registerPath(zipPath, () -> {
                Files.delete(zipPath);

                // Finally, don't forget to delete the DIM-1 and DIM1 folders manually copied by us
                if(server.getServerType().isBukkitSeries()) {
                    if(Files.exists(savePath.resolve("DIM-1"))) Utils.deleteDirectoryRecursively(savePath.resolve("DIM-1"));
                    if(Files.exists(savePath.resolve("DIM1"))) Utils.deleteDirectoryRecursively(savePath.resolve("DIM1"));
                }
            });

            HashMap<String, Object> obj = new HashMap<>();
            obj.put("download", downloadId);
            sendResponse(ctx, obj);
        } catch (Exception e) {
            // Delete the files if some exceptions are thrown
            if(Files.exists(zipPath)) Files.delete(zipPath);
            if(server.getServerType().isBukkitSeries()) {
                if(Files.exists(savePath.resolve("DIM-1"))) Utils.deleteDirectoryRecursively(savePath.resolve("DIM-1"));
                if(Files.exists(savePath.resolve("DIM1"))) Utils.deleteDirectoryRecursively(savePath.resolve("DIM1"));
            }

            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler uploadSave = ctx -> {
        try {
            UploadedFile file = ctx.uploadedFile("file");
            if(file == null || file.size() <= 0) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "File is missing.");
                return;
            }

            final String fileName = file.filename();
            if(!fileName.endsWith(".zip")) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Save file should be a zip.");
                return;
            }

            final Path targetPath = Paths.get("").resolve(fileName.replaceAll(".zip", ""));
            if(Files.exists(targetPath)) {
                sendResponse(ctx, HttpStatus.CONFLICT, "Save name conflict.");
                return;
            }

            // Copy to tmp dir
            final Path filePath = OPanel.TMP_DIR_PATH.resolve(fileName);
            try(InputStream is = file.content()) {
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            // Unzip
            ZipUtility.unzip(filePath, targetPath);
            // Delete zip file
            Files.delete(filePath);

            // Process the unzipped folder
            /*
             * The provided save zip file may contain a save folder
             * or directly contain all the files of the save at the root of the zip file.
             * The logic here is to process the first case. (We'll test it with the `level.dat` file)
             *
             * However, if the fucking user upload something we didn't expect,
             * then just ignore it as if nothing wrong happened.
             */
            if(!Files.exists(targetPath.resolve("level.dat"))) {
                Path folderInside = targetPath.resolve(targetPath.getFileName()).toAbsolutePath();
                if(!Files.exists(folderInside)) {
                    Utils.deleteDirectoryRecursively(targetPath);
                    sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid save file.");
                    return;
                }
                try(Stream<Path> stream = Files.list(folderInside)) {
                    stream.forEach(path -> {
                        try {
                            Utils.copyDirectoryRecursively(path, targetPath.resolve(path.getFileName()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                Utils.deleteDirectoryRecursively(folderInside);
            }

            // Check if the uploaded folder is a valid save
            // If not, delete the uploaded folder
            if(!Files.exists(targetPath.resolve("level.dat"))) {
                Utils.deleteDirectoryRecursively(targetPath);
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid save file.");
                return;
            }

            sendResponse(ctx, HttpStatus.OK);
        } catch (ZipException e) {
            plugin.logger.warn("An illegal save zip is detected! This may cause a zip slip, so it is blocked from unzipping to the server.");
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Invalid save file, zip slip detected.");
            Utils.clearDirectoryRecursively(OPanel.TMP_DIR_PATH);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler editSave = ctx -> {
        final String saveName = ctx.pathParam("saveName");
        SaveEditRequestBodyType reqBody = ctx.bodyAsClass(SaveEditRequestBodyType.class);

        try {
            OPanelSave save = server.getSave(saveName);
            save.setDisplayName(Utils.base64ToString(reqBody.displayName));
            save.setDefaultGameMode(OPanelGameMode.fromString(reqBody.defaultGameMode));
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler deleteSave = ctx -> {
        final String saveName = ctx.pathParam("saveName");
        OPanelSave save = server.getSave(saveName);
        if(save.isRunning() || save.isCurrent()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "You cannot delete current save.");
            return;
        }

        try {
            save.delete();
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    private static class SaveEditRequestBodyType {
        String displayName; // base64
        String defaultGameMode;
    }
}
