package net.opanel.controller;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.web.WebServer;

import java.io.IOException;
import java.io.InputStream;

public class ExtensionPageController extends BaseController {
    private static final String EXTENSION_PAGE = WebServer.ROOT_PATH + "/panel/ext/index.html";

    public ExtensionPageController(OPanel plugin) {
        super(plugin);
    }

    public Handler getExtensionPage = ctx -> {
        String extensionId = ctx.pathParam("extId");
        if(!plugin.getExtensionManager().hasWebIndex(extensionId)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension or extension page was not found.");
            return;
        }

        try(InputStream page = OPanel.class.getClassLoader().getResourceAsStream(EXTENSION_PAGE)) {
            if(page == null) {
                sendResponse(ctx, HttpStatus.NOT_FOUND, "Extension host page was not found.");
                return;
            }
            sendContent(ctx, page.readAllBytes(), ContentType.TEXT_HTML);
        } catch (IOException e) {
            plugin.logger.error("Failed to read extension host page: " + e.getMessage());
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read extension host page.");
        }
    };
}
