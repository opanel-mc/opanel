package net.opanel.api;

import com.sun.net.httpserver.HttpServer;
import net.opanel.OPanel;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ApiServer {
    public final int PORT;

    private final OPanel plugin;
    private HttpServer server;

    public ApiServer(OPanel plugin) {
        this.plugin = plugin;
        PORT = plugin.getConfig().apiServerPort;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext(InfoHandler.route, new InfoHandler(plugin));
        server.setExecutor(null);

        server.start();
        plugin.logger.info("API server is ready on port "+ PORT);
    }
}
