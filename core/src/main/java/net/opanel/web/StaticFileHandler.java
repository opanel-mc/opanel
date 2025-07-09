package net.opanel.web;

import com.sun.net.httpserver.HttpExchange;
import net.opanel.OPanel;
import net.opanel.utils.ServerHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StaticFileHandler extends ServerHandler {
    private final static String ROOT_PATH = "web";
    private final static String DEFAULT_FILE = "index.html";

    public StaticFileHandler(OPanel plugin) {
        super(plugin);
    }

    @Override
    public void handle(HttpExchange req) {
        final String reqPath = req.getRequestURI().getPath();
        if(reqPath.startsWith("/api")) return;

        final boolean hasExtension = reqPath.lastIndexOf(".") > reqPath.lastIndexOf("/");
        String resourcePath = ROOT_PATH + reqPath;

        if(!hasExtension) {
            resourcePath = (
                    resourcePath.endsWith("/")
                    ? (resourcePath + DEFAULT_FILE)
                    : (resourcePath +"/"+ DEFAULT_FILE)
            );
        }

        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if(stream == null) {
            sendResponse(req, 404);
            return;
        }

        try {
            byte[] bytes = stream.readAllBytes();
            stream.close();

            sendContentResponse(req, bytes, getMimeType(resourcePath));
        } catch (IOException e) {
            sendResponse(req, 500);
            e.printStackTrace();
        }
    }

    private String getMimeType(String fileName) {
        if(fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html";
        if(fileName.endsWith(".css")) return "text/css";
        if(fileName.endsWith(".js")) return "text/javascript";
        if(fileName.endsWith(".json")) return "application/json";
        if(fileName.endsWith(".png")) return "image/png";
        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if(fileName.endsWith(".gif")) return "image/gif";
        if(fileName.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
