package net.opanel.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.ServerType;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import net.opanel.utils.ZipUtility;
import net.opanel.web.BaseController;
import org.eclipse.jetty.server.Request;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class SavesController extends BaseController {
    public static final String route = "/api/saves";

    public SavesController(OPanel plugin) {
        super(plugin);
    }

    public void listSaves(Context ctx) {
        if (!authCookie(ctx)) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
            return;
        }

        String saveName = ctx.pathParam("*");
        final OPanelServer server = plugin.getServer();

        if (saveName != null && !saveName.isEmpty() && !saveName.equals("/")) {
            OPanelSave save = server.getSave(saveName);
            if (save == null) {
                sendResponse(ctx, HttpStatus.NOT_FOUND);
                return;
            }

            if (save.isRunning()) {
                server.saveAll();
            }

            Path savePath = save.getPath();
            Path zipPath = OPanel.TMP_DIR_PATH.resolve(save.getName() + ".zip");

            /*
             * Bukkit将下界和末地维度与存档文件夹分开，
             * 因此我们需要在处理存档文件时将它们放在一起
             */
            if (server.getServerType() == ServerType.BUKKIT) {
                try {
                    Utils.copyDirectoryRecursively(Paths.get("").resolve(saveName + "_nether/DIM-1"), savePath.resolve("DIM-1"));
                    Utils.copyDirectoryRecursively(Paths.get("").resolve(saveName + "_the_end/DIM1"), savePath.resolve("DIM1"));
                } catch (IOException e) {
                    e.printStackTrace();
                    sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR);
                    return;
                }
            }

            try {
                ZipUtility.zip(savePath, zipPath);
                byte[] zipContent = Utils.readFile(zipPath);
                ctx.status(HttpStatus.OK).contentType("application/octet-stream").result(zipContent);
                Files.delete(zipPath);

                // 最后，别忘了删除我们手动复制的DIM-1和DIM1文件夹
                if (server.getServerType() == ServerType.BUKKIT) {
                    Utils.deleteDirectoryRecursively(savePath.resolve("DIM-1"));
                    Utils.deleteDirectoryRecursively(savePath.resolve("DIM1"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();
        List<HashMap<String, Object>> saves = new ArrayList<>();

        for (OPanelSave save : server.getSaves()) {
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
    }

    public void uploadSave(Context ctx) {
        if (!authCookie(ctx)) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
            return;
        }

        String saveName = ctx.pathParam("*");
        // 上传新存档（路径为空或仅为"/"）
        if (saveName == null || saveName.isEmpty() || saveName.equals("/")) {
            try {
                // 获取上传的文件
                var uploadedFile = ctx.uploadedFile("file");
                if (uploadedFile == null || uploadedFile.size() <= 0) {
                    sendResponse(ctx, HttpStatus.BAD_REQUEST);
                    return;
                }

                String fileName = uploadedFile.filename();
                if (!fileName.endsWith(".zip")) {
                    sendResponse(ctx, HttpStatus.BAD_REQUEST);
                    return;
                }

                String targetDirName = fileName.replaceAll("\\.zip$", "");
                final Path targetPath = Paths.get("").resolve(targetDirName);
                if (Files.exists(targetPath)) {
                    sendResponse(ctx, HttpStatus.CONFLICT);
                    return;
                }

                // 复制到临时目录
                final Path filePath = OPanel.TMP_DIR_PATH.resolve(fileName);
                try (InputStream is = uploadedFile.content()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }

                // 解压
                ZipUtility.unzip(filePath, targetPath);
                // 删除zip文件
                Files.delete(filePath);

                if (!Files.exists(targetPath.resolve("level.dat"))) {
                    Path folderInside = targetPath.resolve(targetPath.getFileName()).toAbsolutePath();
                    if (!Files.exists(folderInside)) {
                        Utils.deleteDirectoryRecursively(targetPath);
                        sendResponse(ctx, HttpStatus.BAD_REQUEST);
                        return;
                    }
                    try (Stream<Path> stream = Files.list(folderInside)) {
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

                if (!Files.exists(targetPath.resolve("level.dat"))) {
                    Utils.deleteDirectoryRecursively(targetPath);
                    sendResponse(ctx, HttpStatus.BAD_REQUEST);
                    return;
                }

                sendResponse(ctx, HttpStatus.OK);
            } catch (ZipException e) {
                plugin.logger.warn("检测到非法存档zip！这可能导致zip slip，因此阻止其解压到服务器。");
                sendResponse(ctx, HttpStatus.FORBIDDEN);
                Utils.clearDirectoryRecursively(OPanel.TMP_DIR_PATH);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            editSave(ctx);
        }
    }

    private void editSave(Context ctx) {
        String saveName = ctx.pathParam("*");
        final OPanelServer server = plugin.getServer();

        try {
            SaveEditRequestBodyType reqBody = ctx.bodyAsClass(SaveEditRequestBodyType.class);
            OPanelSave save = server.getSave(saveName);

            if (save == null) {
                sendResponse(ctx, HttpStatus.NOT_FOUND);
                return;
            }

            save.setDisplayName(Utils.base64ToString(reqBody.displayName));
            save.setDefaultGameMode(OPanelGameMode.fromString(reqBody.defaultGameMode));
            sendResponse(ctx, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteSave(Context ctx) {
        if (!authCookie(ctx)) {
            sendResponse(ctx, HttpStatus.UNAUTHORIZED);
            return;
        }

        String saveName = ctx.pathParam("*");
        final OPanelServer server = plugin.getServer();

        if (saveName == null || saveName.isEmpty() || saveName.equals("/")) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST);
            return;
        }

        OPanelSave save = server.getSave(saveName);
        if (save == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND);
            return;
        }

        if (save.isRunning() || save.isCurrent()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN);
            return;
        }

        try {
            save.delete();
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static class SaveEditRequestBodyType {
        String displayName; // base64编码
        String defaultGameMode;
    }
}