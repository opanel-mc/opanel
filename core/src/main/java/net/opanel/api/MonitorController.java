package net.opanel.api;

import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.utils.TPS;
import net.opanel.web.BaseController;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.util.HashMap;

public class MonitorController extends BaseController {

    public MonitorController(OPanel plugin) {
        super(plugin);
    }

    public Handler getMonitor = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("cpu", getCpuRate());
        obj.put("mem", getMemRate());
        obj.put("tps", TPS.getRecentTPS());

        sendResponse(ctx, obj);
    };

    private double getCpuRate() {
        SystemInfo si = new SystemInfo();
        double load = si.getHardware().getProcessor().getSystemCpuLoad(500) * 100;
        return Math.round(load);
    }

    private double getMemRate() {
        SystemInfo si = new SystemInfo();
        GlobalMemory gm = si.getHardware().getMemory();

        long total = gm.getTotal();
        long avail = gm.getAvailable();
        long used = total - avail;

        double rate = ((double) used / total) * 100;

        return Math.round(rate);
    }
}
