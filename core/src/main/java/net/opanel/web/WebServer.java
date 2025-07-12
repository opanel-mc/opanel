package net.opanel.web;

import net.opanel.OPanel;
import net.opanel.api.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {
    public final int PORT;

    private final OPanel plugin;
    private Server server;

    public WebServer(OPanel plugin) {
        this.plugin = plugin;
        PORT = plugin.getConfig().webServerPort;
    }

    public void start() throws Exception {
        server = new Server(PORT);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/");
        server.setHandler(ctx);

        // WebSocket
        // ...
        // API
        ctx.addServlet(new ServletHolder(new AuthServlet(plugin)), AuthServlet.route);
        ctx.addServlet(new ServletHolder(new InfoServlet(plugin)), InfoServlet.route);
        ctx.addServlet(new ServletHolder(new IconServlet(plugin)), IconServlet.route);
        ctx.addServlet(new ServletHolder(new PlayersServlet(plugin)), PlayersServlet.route);
        ctx.addServlet(new ServletHolder(new MonitorServlet(plugin)), MonitorServlet.route);
        // Frontend
        ctx.addServlet(new ServletHolder(new StaticFileServlet(plugin)), "/");

        server.start();
        plugin.logger.info("Web server is ready on port "+ PORT);
    }
}
