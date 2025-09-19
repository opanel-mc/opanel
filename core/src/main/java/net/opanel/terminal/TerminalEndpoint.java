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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // Command caching mechanism
    private static List<String> cachedCommands = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_TTL = 30000; // 30 seconds cache TTL

    // Command usage frequency tracking
    private static final Map<String, Integer> commandUsageFrequency = new ConcurrentHashMap<>();
    private static final Map<String, Long> commandLastUsed = new ConcurrentHashMap<>();

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
                    String cleanCommand = command.replaceFirst("/", "");
                    
                    // Track command usage frequency
                    trackCommandUsage(cleanCommand);
                    
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
                        List<String> commands = getCachedCommands();
                        sendMessage(session, new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, commands));
                        return;
                    }
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

    /**
     * Get cached commands with TTL mechanism
     */
    private List<String> getCachedCommands() {
        long currentTime = System.currentTimeMillis();
        if (cachedCommands == null || currentTime - lastCacheTime > CACHE_TTL) {
            cachedCommands = plugin.getServer().getCommands();
            lastCacheTime = currentTime;
        }
        return new ArrayList<>(cachedCommands);
    }

    /**
     * Track command usage frequency
     */
    private void trackCommandUsage(String command) {
        // Extract base command (first word)
        String baseCommand = command.split(" ")[0].toLowerCase();
        
        commandUsageFrequency.merge(baseCommand, 1, Integer::sum);
        commandLastUsed.put(baseCommand, System.currentTimeMillis());
    }

    /**
     * Enhanced autocomplete with fuzzy matching and frequency-based sorting
     */
    private List<String> getEnhancedAutocompleteSuggestions(String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) {
            return getCachedCommands();
        }

        String input = inputText.toLowerCase().trim();
        List<String> allCommands = getCachedCommands();
        
        // Create suggestion candidates with scores
        List<CommandSuggestion> suggestions = new ArrayList<>();
        
        for (String command : allCommands) {
            String lowerCommand = command.toLowerCase();
            int score = calculateMatchScore(input, lowerCommand);
            
            if (score > 0) {
                int frequency = commandUsageFrequency.getOrDefault(command, 0);
                long lastUsed = commandLastUsed.getOrDefault(command, 0L);
                
                suggestions.add(new CommandSuggestion(command, score, frequency, lastUsed));
            }
        }
        
        // Sort by score (descending), then by frequency (descending), then by recent usage
        suggestions.sort((a, b) -> {
            if (a.score != b.score) {
                return Integer.compare(b.score, a.score);
            }
            if (a.frequency != b.frequency) {
                return Integer.compare(b.frequency, a.frequency);
            }
            return Long.compare(b.lastUsed, a.lastUsed);
        });
        
        // Return top 20 suggestions
        return suggestions.stream()
                .limit(20)
                .map(s -> s.command)
                .collect(Collectors.toList());
    }

    /**
     * Calculate match score for fuzzy matching
     * Returns 0 if no match, higher scores for better matches
     */
    private int calculateMatchScore(String input, String command) {
        // Exact match gets highest score
        if (command.equals(input)) {
            return 1000;
        }
        
        // Prefix match gets high score
        if (command.startsWith(input)) {
            return 800 + (100 - input.length()); // Shorter input gets higher score
        }
        
        // Contains match gets medium score
        if (command.contains(input)) {
            return 500 + (100 - Math.abs(command.length() - input.length()));
        }
        
        // Fuzzy match using Levenshtein distance
        int distance = calculateLevenshteinDistance(input, command);
        int maxLength = Math.max(input.length(), command.length());
        
        // If distance is too large, no match
        if (distance > maxLength / 2) {
            return 0;
        }
        
        // Score based on similarity (lower distance = higher score)
        return Math.max(0, 300 - (distance * 50));
    }

    /**
     * Calculate Levenshtein distance for fuzzy matching
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    /**
     * Helper class for command suggestions with scoring
     */
    private static class CommandSuggestion {
        final String command;
        final int score;
        final int frequency;
        final long lastUsed;
        
        CommandSuggestion(String command, int score, int frequency, long lastUsed) {
            this.command = command;
            this.score = score;
            this.frequency = frequency;
            this.lastUsed = lastUsed;
        }
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
