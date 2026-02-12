package net.opanel.endpoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.websocket.*;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.web.JwtManager;
import org.eclipse.jetty.websocket.api.Session;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class BaseEndpoint implements Connectable {
    protected final Javalin app;
    protected final WsConfig ws;
    protected final OPanel plugin;
    protected final OPanelServer server;

    private final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private final ConcurrentHashMap<Session, Set<Consumer<WsMessageContext>>> sessionListeners = new ConcurrentHashMap<>();

    public BaseEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        this.app = app;
        this.ws = ws;
        this.plugin = plugin;
        server = plugin.getServer();

        init();
    }

    private void init() {
        ws.onConnect(ctx -> {
            Session session = ctx.session;

            subscribe(session, Packet.AUTH, String.class, (msgCtx, token) -> {
                final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
                if(token != null && JwtManager.verifyToken(token, hashedRealKey, plugin.getConfig().salt)) {
                    // Register session
                    sessions.add(session);
                    msgCtx.send(new Packet<>(Packet.CONNECT));
                    onConnect(msgCtx);
                } else {
                    msgCtx.closeSession(1008, "Unauthorized.");
                }
            });

            subscribe(session, Packet.PING, msgCtx -> {
                msgCtx.send(new Packet<>(Packet.PONG));
            });
        });

        ws.onMessage(ctx -> {
            if(!sessionListeners.containsKey(ctx.session)) return;
            for(Consumer<WsMessageContext> listener : sessionListeners.get(ctx.session)) {
                try {
                    listener.accept(ctx);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ws.onClose(ctx -> {
            sessions.remove(ctx.session);
            onClose(ctx);
        });

        app.events(event -> {
            event.serverStopping(this::closeAllSessions);
        });
    }

    protected void subscribe(Session session, String type, Consumer<WsMessageContext> cb) {
        subscribe(session, type, Object.class, (ctx, data) -> {
            cb.accept(ctx);
        });
    }

    protected <D> void subscribe(Session session, String type, Class<D> dataClass, BiConsumer<WsMessageContext, D> cb) {
        Set<Consumer<WsMessageContext>> listeners = sessionListeners.computeIfAbsent(session, k -> new CopyOnWriteArraySet<>());
        listeners.add(ctx -> {
            if(ctx.session != session) return;
            if(!sessions.contains(session) && !type.equals(Packet.AUTH)) {
                ctx.closeSession(1008, "Unauthorized.");
                return;
            }

            Packet<?> packet = ctx.messageAsClass(Packet.class);
            if(packet.type.equals(type)) {
                Type realPacketType = TypeToken.getParameterized(Packet.class, dataClass).getType();
                Packet<D> resolvedPacket = ctx.messageAsClass(realPacketType);
                cb.accept(ctx, resolvedPacket.data);
            }
        });
    }

    @Override
    public void onConnect(WsMessageContext ctx) { }

    @Override
    public void onClose(WsCloseContext ctx) { }

    @Override
    public void onError(WsErrorContext ctx) { }

    protected void sendErrorMessage(WsMessageContext ctx, String err) {
        ctx.send(new Packet<>(Packet.ERROR, err));
    }

    protected <D> void sendMessage(Session session, Packet<D> packet) {
        if(!session.isOpen()) return;

        String message = new Gson().toJson(packet);
        try {
            session.getRemote().sendString(message);
        } catch(Exception e) {
            // Use System.err to avoid recursive logging through LogListenerAppender
            System.err.println("[OPanel] Failed to send message to session: " + e.getMessage());
        }
    }

    protected <D> void broadcast(Packet<D> packet) {
        for(Session session : sessions) {
            if(!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            sendMessage(session, packet);
        }
    }

    public void closeAllSessions() {
        for(Session session : sessions) {
            if(session.isOpen()) {
                session.close(1000, "Server is stopping.");
            }
        }
        sessions.clear();
    }
}
