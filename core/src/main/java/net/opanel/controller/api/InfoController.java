package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.time.TPS;
import net.opanel.utils.Utils;
import net.opanel.controller.BaseController;
import oshi.SystemInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class InfoController extends BaseController {
    private final SystemInfo si = new SystemInfo();

    public InfoController(OPanel plugin) {
        super(plugin);
    }

    public Handler getServerInfo = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("favicon", server.getFavicon() != null ? ("/api/icon?t="+ System.currentTimeMillis()) : null);
        obj.put("motd", Utils.stringToBase64(server.getMotd()));
        obj.put("port", server.getPort());
        obj.put("maxPlayerCount", server.getMaxPlayerCount());
        obj.put("whitelist", server.isWhitelistEnabled());
        obj.put("uptime", plugin.getUptimer().getCurrent());

        HashMap<String, Object> ingameTimeObj = new HashMap<>();
        ingameTimeObj.put("current", server.getIngameTime());
        ingameTimeObj.put("doDaylightCycle", server.getGamerules().get("doDaylightCycle"));
        ingameTimeObj.put("paused", TPS.isPaused());
        ingameTimeObj.put("mspt", TPS.getRecentMSPT());
        obj.put("ingameTime", ingameTimeObj);

        HashMap<String, Object> sysObj = new HashMap<>();
        sysObj.put("os", si.getOperatingSystem().toString());
        sysObj.put("arch", System.getProperty("os.arch"));
        sysObj.put("cpuName", si.getHardware().getProcessor().getProcessorIdentifier().getName().trim());
        sysObj.put("cpuCore", si.getHardware().getProcessor().getPhysicalProcessorCount());
        sysObj.put("memory", si.getHardware().getMemory().getTotal());
        sysObj.put("gpus", si.getHardware().getGraphicsCards().stream().map(gpu -> gpu.getName().trim()).toArray());
        sysObj.put("java", System.getProperty("java.version"));
        obj.put("system", sysObj);

        sendResponse(ctx, obj);
    };

    public Handler setMotd = ctx -> {
        try {
            String motd = ctx.bodyAsClass(String.class);
            if(motd.isEmpty() || motd.trim().isEmpty()) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Motd is missing.");
                return;
            }

            server.setMotd(Utils.base64ToString(motd));
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            plugin.logger.error("Failed to update motd: " + e.getMessage());
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (IllegalArgumentException e) {
            plugin.logger.error("Invalid Base64 encoding in motd: " + e.getMessage());
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid Base64 encoding in motd.");
        }
    };
}
