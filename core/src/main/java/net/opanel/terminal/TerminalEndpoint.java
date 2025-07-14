package net.opanel.terminal;

import com.google.gson.Gson;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import net.opanel.OPanel;
import net.opanel.logger.Loggable;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(value = TerminalEndpoint.route, configurator = TerminalEndpoint.Configurator.class)
public class TerminalEndpoint {
    public static final String route = "/terminal";
    private final OPanel plugin;
    private final Loggable logger;
    private final LogListenerManager logListenerManager;

    private final Set<Session> sessions = new HashSet<>();

    public TerminalEndpoint(OPanel plugin) {
        this.plugin = plugin;
        logger = plugin.logger;
        logListenerManager = plugin.getLogListenerManager();

        logListenerManager.addListener(line -> {
            HashMap<String, Object> packet = new HashMap<>();
            packet.put("type", "log");
            packet.put("data", line);
            broadcast(packet);
        });
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Terminal connection established. Session: "+ session.getId());
        sessions.add(session);

        // Send recent logs
        HashMap<String, Object> initialPacket = new HashMap<>();
        initialPacket.put("type", "init");
        initialPacket.put("data", logListenerManager.getRecentLogs());
        sendMessage(session, initialPacket);
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

    private void sendMessage(Session session, HashMap<String, Object> packet) {
        try {
            Gson gson = new Gson();
            session.getBasicRemote().sendObject(gson.toJson(packet));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void broadcast(HashMap<String, Object> packet) {
        sessions.forEach(session -> {
            sendMessage(session, packet);
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
