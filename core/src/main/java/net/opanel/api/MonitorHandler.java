package net.opanel.api;

import com.sun.net.httpserver.HttpExchange;
import net.opanel.OPanel;
import net.opanel.utils.ServerHandler;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.util.HashMap;

public class MonitorHandler extends ServerHandler {
    public static final String route = "/api/monitor";
    private static final long gb = 1024 * 1024 * 1024;

    public MonitorHandler(OPanel plugin) {
        super(plugin);
    }

    @Override
    public void handle(HttpExchange req) {
        if(req.getRequestMethod().equals("OPTIONS")) {
            sendResponse(req, 200);
            return;
        }

        if(!authCookie(req)) {
            sendResponse(req, 401);
            return;
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("cpu", getCpuRate());
        res.put("mem", getMemRate());

        sendResponse(req, res);
    }

    public double getCpuRate() {
        SystemInfo si = new SystemInfo();
        double load = si.getHardware().getProcessor().getSystemCpuLoad(500) * 100;
        return Math.round(load);
    }

    public double getMemRate() {

        SystemInfo si = new SystemInfo();
        GlobalMemory gm = si.getHardware().getMemory();

        long total = gm.getTotal();
        long avail = gm.getAvailable();
        long used = total - avail;

        double rate = ((double) used / total) * 100;

        return Math.round(rate);
    }
}
