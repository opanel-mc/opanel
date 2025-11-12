package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.net.UnknownHostException;
import java.util.HashMap;

public class WhitelistController extends BaseController {
    public WhitelistController(OPanel plugin) {
        super(plugin);
    }

    public Handler getWhitelist = ctx -> {
        final OPanelServer server = plugin.getServer();
        
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("whitelist", server.getWhitelist());
        obj.put("whitelistEnabled", server.hasWhitelist());
        sendResponse(ctx, obj);
    };

    public Handler addWhitelist = ctx -> {
        final OPanelServer server = plugin.getServer();
        
        String name = ctx.queryParam("name");
        if (name == null || name.isEmpty()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Name is missing.");
            return;
        }
        
        if (!Utils.validatePlayerName(name)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid player name.");
            return;
        }
        
        server.addWhitelist(name);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler removeWhitelist = ctx -> {
        final OPanelServer server = plugin.getServer();
        
        String name = ctx.queryParam("name");
        if (name == null || name.isEmpty()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Name is missing.");
            return;
        }
        
        if (!Utils.validatePlayerName(name)) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid player name.");
            return;
        }
        
        server.removeWhitelist(name);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler enableWhitelist = ctx -> {
        final OPanelServer server = plugin.getServer();
        server.setWhitelist(true);
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler disableWhitelist = ctx -> {
        final OPanelServer server = plugin.getServer();
        server.setWhitelist(false);
        sendResponse(ctx, HttpStatus.OK);
    };
}