package net.opanel.controller.openapi;

import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.monitor.MonitorData;

import java.util.HashMap;
import java.util.List;

public class OpenMonitorController extends BaseController {
    public OpenMonitorController(OPanel plugin) {
        super(plugin);
    }

    public Handler getMonitor = ctx -> {
        MonitorData data = null;
        List<MonitorData> history = plugin.getMonitorManager().getHistory();
        if(!history.isEmpty()) {
            data = history.get(history.size() - 1);
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("cpu", data == null ? 0 : data.cpu);
        obj.put("memory", data == null ? 0 : data.memory);
        obj.put("jvmMemory", data == null ? 0 : data.jvmMemory);
        obj.put("tps", data == null ? 0 : data.tps);
        obj.put("networkUpload", 0); // todo
        obj.put("networkDownload", 0); // todo

        sendResponse(ctx, obj);
    };
}
