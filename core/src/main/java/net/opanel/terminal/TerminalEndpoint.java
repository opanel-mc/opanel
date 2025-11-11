package net.opanel.terminal;

import com.google.gson.Gson;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsMessageContext;
import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.web.JwtManager;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.*;
import java.util.Collections;

public class TerminalEndpoint {
    private final OPanel plugin;
    private final LogListenerManager logListenerManager;

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    // To avoid duplicated log listener from registering,
    // which can lead to plenty duplicated logs in the frontend terminal
    private static volatile boolean hasLogListenerRegistered = false;

    public TerminalEndpoint(WsConfig ws, OPanel plugin) {
        this.plugin = plugin;
        logListenerManager = plugin.getLogListenerManager();

        ws.onConnect(this::onConnect);
        ws.onMessage(this::onMessage);
        ws.onClose(this::onClose);

        if(!hasLogListenerRegistered) {
            logListenerManager.addListener(log -> {
                broadcast(new TerminalPacket<>(TerminalPacket.LOG, log));
            });
            hasLogListenerRegistered = true;
        }
    }

    public void onConnect(WsConnectContext ctx) {
        // Connection established silently to avoid log spam
    }

    public void onMessage(WsMessageContext ctx) throws IOException {
        TerminalPacket packet = ctx.messageAsClass(TerminalPacket.class);
        Session session = ctx.session;

        switch(packet.type) {
            case TerminalPacket.AUTH -> {
                String token = (String) packet.data;
                final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
                if(token != null && JwtManager.verifyToken(token, hashedRealKey, plugin.getConfig().salt)) {
                    // Register session
                    sessions.add(session);
                    // Send recent logs
                    ctx.send(new TerminalPacket<>(TerminalPacket.INIT, logListenerManager.getRecentLogs()));
                } else {
                    ctx.closeSession(1008, "Unauthorized.");
                }
            }
            case TerminalPacket.COMMAND -> {
                if(!sessions.contains(session)) {
                    ctx.closeSession(1008, "Unauthorized.");
                    return;
                }
                if(packet.data instanceof String) {
                    String command = (String) packet.data;
                    if(command.startsWith("/")) {
                        command = command.replace("/", "");
                    }
                    plugin.getServer().sendServerCommand(command);
                } else {
                    sendErrorMessage(ctx, "Unexpected type of data.");
                }
            }
            case TerminalPacket.AUTOCOMPLETE -> {
                if(!sessions.contains(session)) {
                    ctx.closeSession(1008, "Unauthorized.");
                    return;
                }

                if(packet.data instanceof Number) {
                    Number arg = (Number) packet.data;
                    if(arg.equals(1.0)) {
                        ctx.send(new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, plugin.getServer().getCommands()));
                        return;
                    }
                    List<OPanelPlayer> players = plugin.getServer().getOnlinePlayers();
                    List<String> nameList = new ArrayList<>();
                    for(OPanelPlayer player : players) {
                        nameList.add(player.getName());
                    }
                    ctx.send(new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, nameList));
                } else {
                    sendErrorMessage(ctx, "Unexpected type of data.");
                }
            }
            default -> sendErrorMessage(ctx, "Unexpected type of packet.");
        }
    }

    public void onClose(WsCloseContext ctx) {
        sessions.remove(ctx.session);
        // Connection closed silently to avoid log spam
    }

    private void sendErrorMessage(WsMessageContext ctx, String err) {
        ctx.send(new TerminalPacket<>(TerminalPacket.ERROR, err));
    }

    private <T> void broadcast(TerminalPacket<T> packet) {
        String message = new Gson().toJson(packet);
        synchronized(sessions) {
            sessions.removeIf(session -> !session.isOpen());
            for(Session session : sessions) {
                try {
                    session.getRemote().sendString(message);
                } catch(Exception e) {
                    // Use System.err to avoid recursive logging through LogListenerAppender
                    System.err.println("[OPanel] Failed to broadcast message to session: " + e.getMessage());
                }
            }
        }
    }

    public static void closeAllSessions() throws IOException {
        for(Session session : sessions) {
            session.close(1000, "Server is stopping.");
        }
        sessions.clear();
    }
}
