package net.opanel.api;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.opanel.OPanel;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelSave;
import net.opanel.utils.Utils;
import net.opanel.utils.ZipUtility;
import net.opanel.web.BaseServlet;
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

public class SavesServlet extends BaseServlet {
    public static final String route = "/api/saves/*";
    private static final MultipartConfigElement multipartConfig = new MultipartConfigElement(
            OPanel.TMP_DIR_PATH.toString(),
            -1L,
            -1L,
            1024 * 1024 // fileSizeThreshold 1MB
    );

    public SavesServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();
        if(reqPath != null && !reqPath.equals("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();

        List<HashMap<String, Object>> saves = new ArrayList<>();
        for(OPanelSave save : server.getSaves()) {
            HashMap<String, Object> saveInfo = new HashMap<>();
            saveInfo.put("name", save.getName());
            saveInfo.put("displayName", save.getDisplayName());
            saveInfo.put("path", save.getPath().toString());
            saveInfo.put("isCurrent", save.isCurrent());
            saveInfo.put("defaultGameMode", save.getDefaultGameMode().getName());
            saves.add(saveInfo);
        }
        obj.put("saves", saves);

        sendResponse(res, obj);
    }

    /** Handle save uploading & save editing */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();

        /** save uploading */
        if(reqPath == null || reqPath.equals("/") || !reqPath.startsWith("/")) {
            req.setCharacterEncoding("utf-8");
            /** @see https://stackoverflow.com/questions/52514462/jetty-no-multipart-config-for-servlet-problem */
            req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);
            try {
                Part filePart = req.getPart("file");
                if(filePart == null || filePart.getSize() <= 0) {
                    sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                final String fileName = filePart.getSubmittedFileName();
                if(!fileName.endsWith(".zip")) {
                    sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                final Path targetPath = Paths.get("").resolve(fileName.replaceAll(".zip", ""));
                if(Files.exists(targetPath)) {
                    sendResponse(res, HttpServletResponse.SC_CONFLICT);
                    return;
                }

                // Copy to tmp dir
                final Path filePath = OPanel.TMP_DIR_PATH.resolve(fileName);
                try(InputStream is = filePart.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                // Unzip
                ZipUtility zipUtility = new ZipUtility(filePath, targetPath);
                zipUtility.unzip();
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
                    try(
                            Stream<Path> unzippedDirStream = Files.list(targetPath);
                            Stream<Path> stream = Files.list(unzippedDirStream.toList().getFirst())
                    ) {
                        stream.forEach(path -> {
                            try {
                                Files.copy(path, targetPath.resolve(path.getFileName()));
                                if(Files.isDirectory(path)) {
                                    Utils.deleteDirectoryRecursively(path);
                                } else {
                                    Files.delete(path);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }

                sendResponse(res, HttpServletResponse.SC_OK);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        /** save editing */
        } else {
            String saveName = reqPath.substring(1);
            SaveEditRequestBodyType reqBody = getRequestBody(req, SaveEditRequestBodyType.class);

            try {
                OPanelSave save = server.getSave(saveName);
                save.setDisplayName(reqBody.displayName);
                save.setDefaultGameMode(OPanelGameMode.fromString(reqBody.defaultGameMode));
                sendResponse(res, HttpServletResponse.SC_OK);
            } catch (IOException e) {
                e.printStackTrace();
                sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String reqPath = req.getPathInfo();
        final OPanelServer server = plugin.getServer();

        if(reqPath == null || reqPath.equals("/") || !reqPath.startsWith("/")) {
            sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String saveName = reqPath.substring(1);
        OPanelSave save = server.getSave(saveName);
        if(save.isCurrent()) {
            sendResponse(res, HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            save.delete();
            sendResponse(res, HttpServletResponse.SC_OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static class SaveEditRequestBodyType {
        String displayName;
        String defaultGameMode;
    }
}
