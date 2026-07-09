package net.opanel.controller.openapi;

import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import oshi.SystemInfo;

import java.util.HashMap;

public class OpenInfoController extends BaseController {
    private final SystemInfo si = new SystemInfo();

    public OpenInfoController(OPanel plugin) {
        super(plugin);
    }

    public Handler getServerInfo = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("motd", server.getMotd());
        obj.put("port", server.getPort());
        obj.put("maxPlayerCount", server.getMaxPlayerCount());
        obj.put("whitelist", server.isWhitelistEnabled());
        obj.put("uptime", plugin.getUptimer().getCurrent());
        obj.put("ingameTime", server.getIngameTime());

        HashMap<String, Object> sysObj = new HashMap<>();
        sysObj.put("os", si.getOperatingSystem().toString());
        sysObj.put("arch", System.getProperty("os.arch"));
        sysObj.put("cpuName", si.getHardware().getProcessor().getProcessorIdentifier().getName().trim());
        sysObj.put("cpuCore", si.getHardware().getProcessor().getPhysicalProcessorCount());
        sysObj.put("cpuThread", si.getHardware().getProcessor().getLogicalProcessorCount());
        sysObj.put("memory", si.getHardware().getMemory().getTotal());
        sysObj.put("jvmMemory", Runtime.getRuntime().maxMemory());
        sysObj.put("gpus", si.getHardware().getGraphicsCards().stream().map(gpu -> gpu.getName().trim()).toArray());
        sysObj.put("java", System.getProperty("java.version"));
        obj.put("system", sysObj);

        sendResponse(ctx, obj);
    };
}
