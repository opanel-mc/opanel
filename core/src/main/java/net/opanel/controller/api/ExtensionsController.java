package net.opanel.controller.api;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.servlet.JavalinServletContext;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.extension.ExtensionManager;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

public class ExtensionsController extends BaseController {
    public ExtensionsController(OPanel plugin) {
        super(plugin);
    }

    public Handler getExtensionResource = ctx -> {
        String extensionId = ctx.pathParam("extId");
        String resourcePath = ctx.pathParamMap().containsKey("resource") ? ctx.pathParam("resource") : "";
        String normalizedPath = Utils.normalizePath(resourcePath);
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
}
