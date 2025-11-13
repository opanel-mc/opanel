package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.web.BaseController;

import java.util.HashMap;

public class GamerulesController extends BaseController {
    public GamerulesController(OPanel plugin) {
        super(plugin);
    }

    public Handler getGamerules = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("gamerules", server.getGamerules());
        sendResponse(ctx, obj);
    };

    public Handler changeGamerule = ctx -> {
        GamerulesEditRequestBodyType reqBody = ctx.bodyAsClass(GamerulesEditRequestBodyType.class);
        if(reqBody.gamerules == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Gamerules is missing.");
            return;
        }

        server.setGamerules(reqBody.gamerules);
        sendResponse(ctx, HttpStatus.OK);
    };

    private static class GamerulesEditRequestBodyType {
        HashMap<String, Object> gamerules;
    }
}
