package net.opanel.api;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.io.InputStream;

public class IconController extends BaseController {
    public IconController(OPanel plugin) {
        super(plugin);
    }

    public Handler getFavicon = ctx -> {
        final OPanelServer server = plugin.getServer();
        byte[] favicon = server.getFavicon();
        if(favicon == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND);
            return;
        }

        sendContent(ctx, favicon, ContentType.IMAGE_PNG);
    };

    public Handler uploadFavicon = ctx -> {
        UploadedFile file = ctx.uploadedFile("file");
        if(file == null || file.size() <= 0) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "File is missing.");
            return;
        }
        if(!file.filename().endsWith(".png")) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Server favicon should be a png.");
            return;
        }

        try(InputStream is = file.content()) {
            final byte[] iconBytes = is.readAllBytes();
            int[] dimensions = Utils.getImageDimensions(iconBytes);
            if(dimensions[0] != 64 || dimensions[1] != 64) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Server favicon should be 64*64 sized.");
                return;
            }

            server.setFavicon(iconBytes);
            sendResponse(ctx, HttpStatus.OK);
        }
    };
}
