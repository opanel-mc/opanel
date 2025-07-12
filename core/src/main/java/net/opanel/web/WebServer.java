package net.opanel.web;

import jakarta.servlet.DispatcherType;
import net.opanel.OPanel;
import net.opanel.api.*;
import net.opanel.terminal.WebSocketServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import java.util.EnumSet;

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

        // CORS configuration
        FilterHolder cors = new FilterHolder(new CrossOriginFilter());
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "http://localhost:3001"); // for dev
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD,OPTIONS");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,X-Credential-Token");
        ctx.addFilter(cors, "/*", EnumSet.of(DispatcherType.REQUEST));

        // API
        ctx.addServlet(new ServletHolder(new AuthServlet(plugin)), AuthServlet.route);
        ctx.addServlet(new ServletHolder(new InfoServlet(plugin)), InfoServlet.route);
        ctx.addServlet(new ServletHolder(new IconServlet(plugin)), IconServlet.route);
        ctx.addServlet(new ServletHolder(new PlayersServlet(plugin)), PlayersServlet.route);
        ctx.addServlet(new ServletHolder(new MonitorServlet(plugin)), MonitorServlet.route);
        // WebSocket
        ctx.addServlet(new ServletHolder(new WebSocketServlet(plugin)), "/socket.io/*");
        // Frontend
        ctx.addServlet(new ServletHolder(new StaticFileServlet(plugin)), "/");

        server.start();
        plugin.logger.info("Web server is ready on port "+ PORT);
    }
}
