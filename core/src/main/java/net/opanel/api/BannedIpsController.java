package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.net.UnknownHostException;
import java.util.HashMap;

public class BannedIpsController extends BaseController {
    public BannedIpsController(OPanel plugin) {
        super(plugin);
    }

    public Handler getBannedIps = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("bannedIps", server.getBannedIps());
        sendResponse(ctx, obj);
    };

    public Handler banIp = ctx -> {
        String ip = ctx.queryParam("ip");
        if(ip == null || !Utils.validateIpv4Address(ip)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Ip is missing or the ip address is illegal.");
            return;
        }

        try {
            server.banIp(ip);
        } catch (UnknownHostException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal ip address.");
            return;
        }
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler pardonIp = ctx -> {
        String ip = ctx.queryParam("ip");
        if(ip == null || !Utils.validateIpv4Address(ip)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Ip is missing or the ip address is illegal.");
            return;
        }

        try {
            server.pardonIp(ip);
        } catch (UnknownHostException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal ip address.");
            return;
        }
        sendResponse(ctx, HttpStatus.OK);
    };
}
