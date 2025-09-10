package net.opanel.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;

import java.io.IOException;
import java.io.InputStream;

public class StaticFileServlet extends BaseServlet {
    public static final String route = "/";

    private final static String ROOT_PATH = "web";
    private final static String DEFAULT_FILE = "index.html";
    private final static String DEFAULT_RSC_FILE = "index.txt";

    public StaticFileServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        final String reqPath = req.getRequestURI();
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

        /** @see https://github.com/vercel/next.js/discussions/59394 */
        // Support for next.js RSC files
        if(reqPath.contains(".txt") && !reqPath.contains(DEFAULT_RSC_FILE) && req.getParameter("_rsc") != null) {
            resourcePath = ROOT_PATH + reqPath.replace(".txt", "/"+ DEFAULT_RSC_FILE);
        }

        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if(stream == null) {
            sendResponse(res, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            byte[] bytes = stream.readAllBytes();
            stream.close();

            sendContentResponse(res, bytes, getMimeType(resourcePath));
        } catch(IOException e) {
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
        if(fileName.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}
