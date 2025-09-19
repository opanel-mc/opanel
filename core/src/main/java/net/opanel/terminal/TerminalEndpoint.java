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
import java.util.Collections;

@ServerEndpoint(value = TerminalEndpoint.route, configurator = TerminalEndpoint.Configurator.class)
public class TerminalEndpoint {
    public static final String route = "/terminal";
    private final OPanel plugin;
    private final Loggable logger;
    private final LogListenerManager logListenerManager;

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    // To avoid duplicated log listener from registering,
    // which can lead to plenty duplicated logs in the frontend terminal
    private static volatile boolean hasLogListenerRegistered = false;

    public TerminalEndpoint(OPanel plugin) {
        this.plugin = plugin;
        logger = plugin.logger;
        logListenerManager = plugin.getLogListenerManager();

        if(!hasLogListenerRegistered) {
            logListenerManager.addListener(log -> {
                broadcast(new TerminalPacket<>(TerminalPacket.LOG, log));
            });
            hasLogListenerRegistered = true;
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        // Connection established silently to avoid log spam
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        try {
            Gson gson = new Gson();
            TerminalPacket packet = gson.fromJson(message, TerminalPacket.class);

            switch(packet.type) {
                case TerminalPacket.AUTH -> {
                    String token = (String) packet.data; // salted hashed 3
                    final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
                    if(token != null && token.equals(Utils.md5(plugin.getConfig().salt + hashedRealKey))) {
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
                    
                    // Command validation and security checks
                    if (command.length() > 1000) {
                        sendErrorMessage(session, "Command too long. Maximum length is 1000 characters.");
                        return;
                    }
                    
                    if (command.trim().isEmpty()) {
                        sendErrorMessage(session, "Empty command not allowed.");
                        return;
                    }
                    
                    String cleanCommand = command.replaceFirst("/", "");
                    
                    plugin.getServer().sendServerCommand(cleanCommand);
                }
                case TerminalPacket.AUTOCOMPLETE -> {
                    if(!sessions.contains(session)) {
                        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized."));
                        return;
                    }
                    
                    // Handle different autocomplete request types
                    if(packet.data instanceof String inputText) {
                        // New enhanced autocomplete with fuzzy matching and frequency sorting
                        List<String> suggestions = getEnhancedAutocompleteSuggestions(inputText);
                        sendMessage(session, new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, suggestions));
                        return;
                    }
                    
                    if(!(packet.data instanceof Number arg)) {
                        sendErrorMessage(session, "Unexpected type of data.");
                        return;
                    }

                    if(arg.equals(1.0)) {
                        // Return sorted commands by priority for better user experience
                        List<String> commands = plugin.getServer().getCommandsSorted();
                        sendMessage(session, new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, commands));
                        return;
                    }
                    
                    // Return player names for command arguments
                    sendMessage(session, new TerminalPacket<>(
                            TerminalPacket.AUTOCOMPLETE,
                            plugin.getServer().getOnlinePlayers().stream().map(OPanelPlayer::getName).toList()
                    ));
                }
                default -> sendErrorMessage(session, "Unexpected type of packet.");
            }
        } catch(JsonSyntaxException e) {
            // Use System.err to avoid recursive logging through LogListenerAppender
            System.err.println("[OPanel] JSON parsing error in terminal: " + e.getMessage());
            sendErrorMessage(session, "Json syntax error: "+ e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        // Connection closed silently to avoid log spam
    }

    private <T> void sendMessage(Session session, TerminalPacket<T> packet) {
        if(session.isOpen()) {
            try {
                Gson gson = new Gson();
                session.getBasicRemote().sendText(gson.toJson(packet));
            } catch(IOException e) {
                // Use System.err to avoid recursive logging through LogListenerAppender
                System.err.println("[OPanel] Failed to send WebSocket message: " + e.getMessage());
                sessions.remove(session);
            }
        }
    }

    private void sendErrorMessage(Session session, String err) {
        sendMessage(session, new TerminalPacket<>(TerminalPacket.ERROR, err));
    }

    // Cache for commands with TTL
    private static final long CACHE_TTL = 30000; // 30 seconds
    private List<String> cachedCommands;
    private long lastCacheTime = 0;

    /**
     * Get cached commands with TTL mechanism
     */
    private List<String> getCachedCommands() {
        long currentTime = System.currentTimeMillis();
        if (cachedCommands == null || currentTime - lastCacheTime > CACHE_TTL) {
            cachedCommands = plugin.getServer().getCommandsSorted(); // Use sorted commands
            lastCacheTime = currentTime;
        }
        return new ArrayList<>(cachedCommands);
    }

    /**
     * Enhanced autocomplete with fuzzy matching and frequency sorting
     */
    private List<String> getEnhancedAutocompleteSuggestions(String inputText) {
        List<String> commands = getCachedCommands();
        String input = inputText.toLowerCase().trim();
        
        if (input.isEmpty()) {
            return commands.subList(0, Math.min(10, commands.size()));
        }
        
        return commands.stream()
            .filter(cmd -> cmd.toLowerCase().contains(input))
            .sorted((a, b) -> {
                String aLower = a.toLowerCase();
                String bLower = b.toLowerCase();
                
                // Exact match first
                if (aLower.equals(input)) return -1;
                if (bLower.equals(input)) return 1;
                
                // Starts with input
                boolean aStarts = aLower.startsWith(input);
                boolean bStarts = bLower.startsWith(input);
                if (aStarts && !bStarts) return -1;
                if (bStarts && !aStarts) return 1;
                
                // Shorter commands first
                return Integer.compare(a.length(), b.length());
            })
            .limit(20)
            .toList();
    }

    private <T> void broadcast(TerminalPacket<T> packet) {
        String message = new Gson().toJson(packet);
        synchronized (sessions) {
            sessions.removeIf(session -> !session.isOpen());
            for(Session session : sessions) {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch(Exception e) {
                    // Use System.err to avoid recursive logging through LogListenerAppender
                    System.err.println("[OPanel] Failed to broadcast message to session: " + e.getMessage());
                }
            }
        }
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
