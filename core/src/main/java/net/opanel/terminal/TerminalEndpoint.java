package net.opanel.terminal;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import net.opanel.OPanel;
import net.opanel.logger.Loggable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(value = TerminalEndpoint.route, configurator = TerminalEndpoint.Configurator.class)
public class TerminalEndpoint {
    public static final String route = "/terminal";
    private final OPanel plugin;
    private final Loggable logger;

    private Set<Session> sessions = new HashSet<>();

    public TerminalEndpoint(OPanel plugin) {
        this.plugin = plugin;
        logger = plugin.logger;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Terminal connection established. Session: "+ session.getId());
        sessions.add(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        //
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("Terminal connection closed. Session: "+ session.getId());
        sessions.remove(session);
    }

    private void broadcast(String message) {
        sessions.forEach(session -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {
        private static OPanel pluginInstance;

        public static void setPlugin(OPanel plugin) {
            pluginInstance = plugin;
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
            if(TerminalEndpoint.class.equals(endpointClass)) {
                if(pluginInstance == null) {
                    throw new IllegalStateException("Plugin instance has not been set in the EndpointConfigurator.");
                }
                return (T) new TerminalEndpoint(pluginInstance);
            }
            throw new InstantiationException("The provided endpoint class is not equal to TerminalEndpoint.");
        }
    }
}
