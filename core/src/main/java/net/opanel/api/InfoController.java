package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.utils.TPS;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InfoController extends BaseController {
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

        List<HashMap<String, Object>> players = server.getOnlinePlayers().stream()
                .map(player -> {
                    HashMap<String, Object> playerInfo = new HashMap<>();
                    playerInfo.put("name", player.getName());
                    playerInfo.put("uuid", player.getUUID());
                    playerInfo.put("gamemode", player.getGameMode().getName());
                    playerInfo.put("ping", player.getPing());
                    return playerInfo;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        obj.put("onlinePlayers", players);

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
