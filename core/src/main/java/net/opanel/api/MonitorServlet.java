package net.opanel.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.web.BaseServlet;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.util.HashMap;

public class MonitorServlet extends BaseServlet {

    public static final String route = "/api/monitor";
    private static final long gb = 1024 * 1024 * 1024;

    public MonitorServlet(OPanel plugin) {
        super(plugin);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        if(req.getMethod().equals("OPTIONS")) {
            sendResponse(res, HttpServletResponse.SC_OK);
            return;
        }

        if(!authCookie(req)) {
            sendResponse(res, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("cpu", getCpuRate());
        obj.put("mem", getMemRate());

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
