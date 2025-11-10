package net.opanel.api;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import net.opanel.web.BaseServlet;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.io.InputStream;

public class IconServlet extends BaseServlet {
    public static final String route = "/api/icon";
    private static final MultipartConfigElement multipartConfig = new MultipartConfigElement(
            OPanel.TMP_DIR_PATH.toString(),
            -1L,
            -1L,
            1024 * 1024 // fileSizeThreshold 1MB
    );

    public IconServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        final OPanelServer server = plugin.getServer();
        byte[] favicon = server.getFavicon();
        if(favicon == null) {
            sendResponse(res, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        sendContentResponse(res, favicon, "image/png");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final OPanelServer server = plugin.getServer();
        req.setCharacterEncoding("utf-8");
        req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);
        try {
            Part filePart = req.getPart("file");
            if(filePart == null || filePart.getSize() <= 0) {
                sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if(!filePart.getSubmittedFileName().endsWith(".png")) {
                sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try(InputStream is = filePart.getInputStream()) {
                final byte[] iconBytes = is.readAllBytes();
                int[] dimensions = Utils.getImageDimensions(iconBytes);
                if(dimensions[0] != 64 || dimensions[1] != 64) {
                    sendResponse(res, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                server.setFavicon(iconBytes);
                sendResponse(res, HttpServletResponse.SC_OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
