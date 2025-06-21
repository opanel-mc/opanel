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
        String reqPath = req.getRequestURI().getPath();

        if(reqPath.equals("/")) {
            reqPath += DEFAULT_FILE;
        }

        String resourcePath = ROOT_PATH + reqPath;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if(stream == null && !resourcePath.contains(".")) {
            resourcePath = resourcePath +"/"+ DEFAULT_FILE;
            stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        }

        if(stream == null) {
            sendResponse(req, 404, "Not found");
            return;
        }

        try {
            byte[] bytes = stream.readAllBytes();
            stream.close();

            req.getResponseHeaders().set("Content-Type", getMimeType(resourcePath));
            req.sendResponseHeaders(200, bytes.length);

            try(OutputStream os = req.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            sendResponse(req, 500, "Internal server error");
            e.printStackTrace();
        }
    }

    private String getMimeType(String fileName) {
        if(fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html";
        if(fileName.endsWith(".css")) return "text/css";
        if(fileName.endsWith(".js")) return "application/javascript";
        if(fileName.endsWith(".json")) return "application/json";
        if(fileName.endsWith(".png")) return "image/png";
        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if(fileName.endsWith(".gif")) return "image/gif";
        if(fileName.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
