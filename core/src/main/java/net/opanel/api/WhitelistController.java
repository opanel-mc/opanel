package net.opanel.api;

import com.google.gson.reflect.TypeToken;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelWhitelist;
import net.opanel.web.BaseController;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

public class WhitelistController extends BaseController {
    private static final Type whitelistType = new TypeToken<List<OPanelWhitelist.OPanelWhitelistEntry>>() {}.getType();

    public WhitelistController(OPanel plugin) {
        super(plugin);
    }

    public Handler getWhitelist = ctx -> {
        try {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("whitelist", server.getWhitelist().getEntries());
            sendResponse(ctx, obj);
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler enableWhitelist = ctx -> {
        server.setWhitelistEnabled(true);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler disableWhitelist = ctx -> {
        server.setWhitelistEnabled(false);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler writeWhitelist = ctx -> {
        List<OPanelWhitelist.OPanelWhitelistEntry> entries = ctx.bodyAsClass(whitelistType);
        if(entries == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid whitelist payload.");
            return;
        }
        try {
            server.getWhitelist().write(entries);
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler addWhitelistEntry = ctx -> {
        String name = ctx.queryParam("name");
        String uuid = ctx.queryParam("uuid");
        if(name == null || uuid == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Missing name or uuid.");
            return;
        }
        try {
            server.getWhitelist().add(new OPanelWhitelist.OPanelWhitelistEntry(name, uuid));
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler removeWhitelistEntry = ctx -> {
        String name = ctx.queryParam("name");
        String uuid = ctx.queryParam("uuid");
        if(name == null || uuid == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Missing name or uuid.");
            return;
        }
        try {
            server.getWhitelist().remove(new OPanelWhitelist.OPanelWhitelistEntry(name, uuid));
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };
}
