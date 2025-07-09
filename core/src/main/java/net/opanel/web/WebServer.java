package net.opanel.web;

import com.sun.net.httpserver.HttpServer;
import net.opanel.OPanel;
import net.opanel.api.*;

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
        server.createContext(AuthHandler.route, new AuthHandler(plugin));
        server.createContext(InfoHandler.route, new InfoHandler(plugin));
        server.createContext(IconHandler.route, new IconHandler(plugin));
        server.createContext(PlayersHandler.route, new PlayersHandler(plugin));
        server.createContext("/", new StaticFileHandler(plugin));
        server.setExecutor(null);

        server.start();
        plugin.logger.info("Web server is ready on port "+ PORT);
    }
}
