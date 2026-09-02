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
        obj.put("cpu", data == null ? 0 : data.cpu());
        obj.put("memory", data == null ? 0 : data.memory());
        obj.put("jvmMemory", data == null ? 0 : data.jvmMemory());
        obj.put("tps", data == null ? 0 : data.tps());
        obj.put("networkUpload", data == null ? 0 : data.networkUpload());
        obj.put("networkDownload", data == null ? 0 : data.networkDownload());
        obj.put("diskRead", data == null ? 0 : data.diskRead());
        obj.put("diskWrite", data == null ? 0 : data.diskWrite());

        sendResponse(ctx, obj);
    };
}
