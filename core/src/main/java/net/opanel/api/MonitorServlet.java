package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.utils.TPS;
import net.opanel.web.BaseServlet;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.util.HashMap;

public class MonitorServlet extends BaseServlet {

    public static final String route = "/api/monitor";

    public MonitorServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("cpu", getCpuRate());
        obj.put("mem", getMemRate());
        obj.put("tps", TPS.getRecentTPS());

        sendResponse(res, obj);
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
