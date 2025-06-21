package net.opanel.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.opanel.OPanel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFileHandler implements HttpHandler {
    private final static String ROOT_PATH = "web";
    private final static String DEFAULT_FILE = "index.html";

    @Override
    public void handle(HttpExchange req) throws IOException {
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
            sendError(req, 404, "Not found");
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
            sendError(req, 500, "Internal server error");
            e.printStackTrace();
        }
    }

    private void sendError(HttpExchange req, int code, String msg) {
        try {
            req.sendResponseHeaders(code, msg.length());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try(OutputStream os = req.getResponseBody()) {
            os.write(msg.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
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
