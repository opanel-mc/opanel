package net.opanel.web;

import com.sun.net.httpserver.HttpServer;
import net.opanel.OPanel;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {
    public final int PORT;

    private final OPanel plugin;
    private HttpServer server;

    public WebServer(OPanel plugin) {
        this.plugin = plugin;
        PORT = plugin.getConfig().webServerPort;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null);

        server.start();
        plugin.logger.info("Web server is ready on port "+ PORT);
    }
}
