package net.opanel.terminal;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.logger.Loggable;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.util.*;

@ServerEndpoint(value = TerminalEndpoint.route, configurator = TerminalEndpoint.Configurator.class)
public class TerminalEndpoint {
    public static final String route = "/terminal";
    private final OPanel plugin;
    private final Loggable logger;
    private final LogListenerManager logListenerManager;

    private static final Set<Session> sessions = new HashSet<>();

    // To avoid duplicated log listener from registering,
    // which can lead to plenty duplicated logs in the frontend terminal
    private static boolean hasLogListenerRegistered = false;

    public TerminalEndpoint(OPanel plugin) {
        this.plugin = plugin;
        logger = plugin.logger;
        logListenerManager = plugin.getLogListenerManager();

        if(!hasLogListenerRegistered) {
            logListenerManager.addListener(line -> {
                broadcast(new TerminalPacket<>(TerminalPacket.LOG, line));
            });
            hasLogListenerRegistered = true;
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        // logger.info("Terminal connection established. Session: "+ session.getId());

    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        try {
            Gson gson = new Gson();
            TerminalPacket packet = gson.fromJson(message, TerminalPacket.class);

            switch(packet.type) {
                case TerminalPacket.AUTH -> {
                    String token = (String) packet.data; // hashed 2
                    final String hashedRealKey = Utils.md5(Utils.md5(plugin.getConfig().accessKey)); // hashed 2
                    if(token != null && token.equals(hashedRealKey)) {
                        // Register session
                        sessions.add(session);
                        // Send recent logs
                        sendMessage(session, new TerminalPacket<>(TerminalPacket.INIT, logListenerManager.getRecentLogs()));
                    } else {
                        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized."));
                    }
                }
                case TerminalPacket.COMMAND -> {
                    if(!sessions.contains(session)) {
                        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized."));
                        return;
                    }
                    if(!(packet.data instanceof String command)) {
                        sendErrorMessage(session, "Unexpected type of data.");
                        return;
                    }
                    plugin.getServer().sendServerCommand(command);
                }
                case TerminalPacket.AUTOCOMPLETE -> {
                    if(!sessions.contains(session)) {
                        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized."));
                        return;
                    }
                    if(!(packet.data instanceof Number arg)) {
                        sendErrorMessage(session, "Unexpected type of data.");
                        return;
                    }

                    if(arg.equals(1.0)) {
                        sendMessage(session, new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, plugin.getServer().getCommands()));
                        return;
                    }
                    sendMessage(session, new TerminalPacket<>(
                            TerminalPacket.AUTOCOMPLETE,
                            plugin.getServer().getOnlinePlayers().stream().map(OPanelPlayer::getName).toList()
                    ));
                }
                default -> sendErrorMessage(session, "Unexpected type of packet.");
            }
        } catch (JsonSyntaxException e) {
            sendErrorMessage(session, "Json syntax error: "+ e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        // logger.info("Terminal connection closed. Session: "+ session.getId());
        sessions.remove(session);
    }

    private <T> void sendMessage(Session session, TerminalPacket<T> packet) {
        try {
            Gson gson = new Gson();
            session.getBasicRemote().sendObject(gson.toJson(packet));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void sendErrorMessage(Session session, String err) {
        sendMessage(session, new TerminalPacket<>(TerminalPacket.ERROR, err));
    }

    private <T> void broadcast(TerminalPacket<T> packet) {
        sessions.forEach(session -> {
            sendMessage(session, packet);
        });
    }

    public static void closeAllSessions() throws IOException {
        for(Session session : sessions) {
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Server is stopping."));
        }
        sessions.clear();
    }

    public static class Configurator extends ServerEndpointConfig.Configurator {
        private static OPanel pluginInstance;

        public static void setPlugin(OPanel plugin) {
            pluginInstance = plugin;
        }

        @Override
        @SuppressWarnings("unchecked")
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
