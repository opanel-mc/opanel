package net.opanel.endpoint;

import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.opanel.OPanel;
import net.opanel.monitor.MonitorData;
import net.opanel.monitor.MonitorManager;

import java.util.List;
import java.util.function.Consumer;

public class MonitorEndpoint extends BaseEndpoint {
    private static class MonitorPacket<D> extends Packet<D> {
        public static final String INIT = "init";
        public static final String UPDATE = "update";

        public MonitorPacket(String type, D data) {
            super(type, data);
        }
    }

    private final MonitorManager monitorManager;
    private final Consumer<MonitorData> updateListener;

    public MonitorEndpoint(Javalin app, WsConfig ws, OPanel plugin) {
        super(app, ws, plugin);

        monitorManager = plugin.getMonitorManager();

        updateListener = (MonitorData data) -> broadcast(new MonitorPacket<>(MonitorPacket.UPDATE, data));
        monitorManager.addUpdateListener(updateListener);
    }

    @Override
    public void onConnect(WsContext ctx) {
        List<MonitorData> history = monitorManager.getHistory(parseInitLimit(ctx.queryParam("limit")));
        ctx.send(new MonitorPacket<>(MonitorPacket.INIT, history));
    }

    @Override
    public void onShutdown() {
        monitorManager.removeUpdateListener(updateListener);
    }

    private int parseInitLimit(String initLimit) {
        if(initLimit == null || initLimit.isBlank()) {
            return MonitorManager.MAX_HISTORY_SIZE;
        }

        try {
            return Integer.parseInt(initLimit);
        } catch (NumberFormatException ignored) {
            return MonitorManager.MAX_HISTORY_SIZE;
        }
    }
}
