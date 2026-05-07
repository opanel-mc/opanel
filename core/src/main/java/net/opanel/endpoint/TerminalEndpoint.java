package net.opanel.endpoint;

import io.javalin.Javalin;
import io.javalin.websocket.*;
import net.opanel.OPanel;
import net.opanel.terminal.LogListenerManager;
import net.opanel.terminal.MCDRConnector;
import org.eclipse.jetty.websocket.api.Session;

import java.util.concurrent.atomic.AtomicBoolean;

public class TerminalEndpoint extends BaseEndpoint {
    private static class TerminalPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String LOG = "log";
        public static final String MCDRLOG = "mcdr-log";
        public static final String AUTOCOMPLETE = "autocomplete";
        public static final String COMMAND = "command";

        public TerminalPacket(String type, D data) {
            super(type, data);
        }
    }

    private static class AutocompletePacketData {
        String command;
        int argIndex; // starts from 1
    }

    private final LogListenerManager logListenerManager;
    private MCDRConnector mcdrConnector;

    // To avoid duplicated log listener from registering,
    // which can lead to plenty duplicated logs in the frontend terminal
    private static final AtomicBoolean hasLogListenerRegistered = new AtomicBoolean(false);
    private static final AtomicBoolean hasMCDRLogListenerRegistered = new AtomicBoolean(false);

    public TerminalEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        super(app, ws, plugin);

        logListenerManager = plugin.getLogListenerManager();
        if(hasLogListenerRegistered.compareAndSet(false, true)) {
            logListenerManager.addListener(log -> {
                broadcast(new TerminalPacket<>(TerminalPacket.LOG, log));
            });
        }

        if(OPanel.isMCDRBridgeActive()) {
            mcdrConnector = new MCDRConnector(plugin, plugin.getConfig().mcdrSocketPort);
            mcdrConnector.connect();
            if(hasMCDRLogListenerRegistered.compareAndSet(false, true)) {
                mcdrConnector.addListener(log -> {
                    broadcast(new TerminalPacket<>(TerminalPacket.MCDRLOG, log));
                });
            }
        }
    }

    @Override
    public void onConnect(WsContext ctx) {
        Session session = ctx.session;

        ctx.send(new TerminalPacket<>(TerminalPacket.INIT, logListenerManager.getRecentLogs()));

        subscribe(session, TerminalPacket.COMMAND, String.class, (msgCtx, command) -> {
            if(OPanel.isMCDRBridgeActive() && command.startsWith("!!") && mcdrConnector != null) {
                mcdrConnector.send(command);
                return;
            }
            if(command.startsWith("/")) {
                command = command.substring(1);
            }
            plugin.getServer().sendServerCommand(command);
        });

        subscribe(session, TerminalPacket.AUTOCOMPLETE, AutocompletePacketData.class, (msgCtx, data) -> {
            if(data.argIndex == 1) {
                ctx.send(new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, plugin.getServer().getCommands()));
                return;
            }
            ctx.send(new TerminalPacket<>(TerminalPacket.AUTOCOMPLETE, plugin.getServer().getCommandTabList(data.argIndex, data.command)));
        });
    }

    @Override
    public void onShutdown() {
        logListenerManager.clearListeners();
        if(mcdrConnector != null) {
            mcdrConnector.clearListeners();
            mcdrConnector.dispose();
            mcdrConnector = null;
        }
        hasLogListenerRegistered.set(false);
        hasMCDRLogListenerRegistered.set(false);
    }
}
