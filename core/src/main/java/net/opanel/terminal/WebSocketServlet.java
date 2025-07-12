package net.opanel.terminal;

import io.socket.engineio.server.EngineIoServer;
import io.socket.engineio.server.EngineIoServerOptions;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.logger.Loggable;
import net.opanel.web.BaseServlet;

import java.io.IOException;

public class WebSocketServlet extends BaseServlet {
    private final EngineIoServer mEngineIoServer;
    private final SocketIoServer mSocketIoServer;

    private final Loggable logger;

    public WebSocketServlet(OPanel plugin) {
        super(plugin);
        logger = plugin.logger;

        final EngineIoServerOptions opts = EngineIoServerOptions.newFromDefault();
        opts.setCorsHandlingDisabled(true); // CORS will be handled by jetty

        mEngineIoServer = new EngineIoServer(opts);
        mSocketIoServer = new SocketIoServer(mEngineIoServer);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        SocketIoNamespace namespace = mSocketIoServer.namespace("/");

        namespace.on("connection", args -> {
            SocketIoSocket socket = (SocketIoSocket) args[0];
            logger.info("Terminal socket connected. ID: "+ socket.getId());

            socket.on("client-message", msgArgs -> {
                String msg = (String) msgArgs[0];

                //
            });

            socket.on("disconnect", disconnectArgs -> {
                logger.info("Terminal socket disconnected.");
            });
        });
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        mEngineIoServer.handleRequest(req, res);
    }
}
