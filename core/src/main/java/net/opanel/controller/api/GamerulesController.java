package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelDimension;
import net.opanel.controller.BaseController;

import java.util.HashMap;

public class GamerulesController extends BaseController {
    public GamerulesController(OPanel plugin) {
        super(plugin);
    }

    public Handler getGamerules = ctx -> {
        final String dimName = ctx.pathParam("dimName");
        OPanelDimension dimension = OPanelDimension.fromString(dimName);
        if(dimension == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal dimension name.");
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("gamerules", server.getGamerules(dimension));
        sendResponse(ctx, obj);
    };

    public Handler changeGamerule = ctx -> {
        final String dimName = ctx.pathParam("dimName");
        OPanelDimension dimension = OPanelDimension.fromString(dimName);
        if(dimension == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal dimension name.");
            return;
        }

        GamerulesEditRequestBodyType reqBody = ctx.bodyAsClass(GamerulesEditRequestBodyType.class);
        if(reqBody.gamerules() == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Gamerules is missing.");
            return;
        }

        server.setGamerules(dimension, reqBody.gamerules());
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler patchGamerule = ctx -> { // for mcp
        final String dimName = ctx.pathParam("dimName");
        OPanelDimension dimension = OPanelDimension.fromString(dimName);
        if(dimension == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal dimension name.");
            return;
        }

        final String key = ctx.queryParam("key");
        final String value = ctx.queryParam("value");
        if(key == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Key is missing.");
            return;
        }
        if(value == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Value is missing.");
            return;
        }

        HashMap<String, Object> gamerules = server.getGamerules(dimension);
        if(!gamerules.containsKey(key)) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the specified gamerule.");
            return;
        }
        gamerules.put(key, value);
        server.setGamerules(dimension, gamerules);
        sendResponse(ctx, HttpStatus.OK);
    };

    private record GamerulesEditRequestBodyType(HashMap<String, Object> gamerules) {}
}
