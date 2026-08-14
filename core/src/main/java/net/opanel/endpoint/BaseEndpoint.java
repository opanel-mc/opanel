package net.opanel.endpoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.websocket.*;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.web.JwtManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.lang.reflect.Type;
import java.nio.channels.WritePendingException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class BaseEndpoint implements Connectable {
    private static final Gson GSON = new Gson();
    // Bounds Jetty's otherwise-unbounded outgoing frame queue (default -1) so a stalled client cannot exhaust memory.
    // This bound counts frames, not bytes.
    private static final int MAX_OUTGOING_FRAMES = 1024;

    protected final Javalin app;
    protected final WsConfig ws;
    protected final OPanel plugin;
    protected final OPanelServer server;

    private final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private final ConcurrentHashMap<Session, Set<Consumer<WsMessageContext>>> sessionListeners = new ConcurrentHashMap<>();
    private final Set<Session> slowConsumerSessions = ConcurrentHashMap.newKeySet();

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

            String token = ctx.cookie("token");
            final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
            if(token == null || !JwtManager.verifyToken(token, hashedRealKey, plugin.getConfig().salt)) {
                ctx.closeSession(1008, "Unauthorized.");
                return;
            }
            // Register session
            session.getRemote().setMaxOutgoingFrames(MAX_OUTGOING_FRAMES);
            sessions.add(session);
            ctx.send(new Packet<>(Packet.CONNECT));
            onConnect(ctx);

            subscribe(session, Packet.PING, msgCtx -> {
                msgCtx.send(new Packet<>(Packet.PONG));
            });
        });

        ws.onMessage(ctx -> {
            Set<Consumer<WsMessageContext>> listeners = sessionListeners.get(ctx.session);
            if(listeners == null) return;
            for(Consumer<WsMessageContext> listener : listeners) {
                try {
                    listener.accept(ctx);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ws.onClose(ctx -> {
            cleanupSession(ctx.session);
            onClose(ctx);
        });

        app.events(event -> {
            event.serverStopping(() -> {
                closeAllSessions();
                onShutdown();
            });
        });
    }

    protected void subscribe(Session session, String type, Consumer<WsMessageContext> cb) {
        subscribe(session, type, Object.class, (ctx, data) -> {
            cb.accept(ctx);
        });
    }

    protected <D> void subscribe(Session session, String type, Class<D> dataClass, BiConsumer<WsMessageContext, D> cb) {
        if(!sessions.contains(session)) return;

        Set<Consumer<WsMessageContext>> listeners = sessionListeners.computeIfAbsent(session, k -> new CopyOnWriteArraySet<>());
        listeners.add(ctx -> {
            if(ctx.session != session) return;
            if(!sessions.contains(session)) {
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
    public void onConnect(WsContext ctx) { }

    @Override
    public void onClose(WsCloseContext ctx) { }

    @Override
    public void onError(WsErrorContext ctx) { }

    @Override
    public void onShutdown() { }

    protected void sendErrorMessage(WsContext ctx, HttpStatus status) {
        ctx.send(new Packet<>(Packet.ERROR, status.getCode()));
    }

    protected <D> void sendMessage(Session session, Packet<D> packet) {
        if(!session.isOpen() || slowConsumerSessions.contains(session)) return;
        sendMessage(session, GSON.toJson(packet));
    }

    private void sendMessage(Session session, String message) {
        if(!session.isOpen() || slowConsumerSessions.contains(session)) return;

        try {
            session.getRemote().sendString(message, new WriteCallback() {
                @Override
                public void writeFailed(Throwable t) {
                    // Minecraft reroutes System.err into the server log, so the send path must remain silent.
                    if(t instanceof WritePendingException && slowConsumerSessions.add(session)) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                session.close(1013, "Slow consumer");
                            } catch(Throwable ignored) {
                                try {
                                    session.disconnect();
                                } catch(Throwable ignoredDisconnectFailure) { }
                            }
                        });
                    }
                }

                @Override
                public void writeSuccess() { }
            });
        } catch(Throwable ignored) { }
    }

    protected <D> void broadcast(Packet<D> packet) {
        if(sessions.isEmpty()) return;
        String message = GSON.toJson(packet);
        for(Session session : sessions) {
            if(!session.isOpen()) {
                cleanupSession(session);
                continue;
            }
            sendMessage(session, message);
        }
    }

    public void closeAllSessions() {
        for(Session session : sessions) {
            if(session.isOpen()) {
                session.close(1000, "Server is stopping.");
            }
        }
        sessions.clear();
        sessionListeners.clear();
        slowConsumerSessions.clear();
    }

    private void cleanupSession(Session session) {
        sessions.remove(session);
        sessionListeners.remove(session);
        slowConsumerSessions.remove(session);
    }
}
