package net.opanel.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.features.CodeOfConductFeature;
import net.opanel.utils.Utils;
import net.opanel.web.BaseController;

import java.io.IOException;
import java.util.HashMap;

public class ControlController extends BaseController {
    public ControlController(OPanel plugin) {
        super(plugin);
    }

    public Handler getServerProperties = ctx -> {
        try {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("properties", Utils.stringToBase64(OPanelServer.getPropertiesContent()));
            sendResponse(ctx, obj);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler setServerProperties = ctx -> {
        try {
            final String properties = ctx.bodyAsClass(String.class);
            if(properties.isEmpty()) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "server.properties content is missing.");
                return;
            }

            OPanelServer.writePropertiesContent(Utils.base64ToString(properties));
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler getCodeOfConducts = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();

        if(!(server instanceof CodeOfConductFeature)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Minecraft versions lower than 1.21.9 don't support server code-of-conduct.");
            return;
        }

        try {
            HashMap<String, String> codeOfConducts = ((CodeOfConductFeature) server).getCodeOfConducts();
            codeOfConducts.replaceAll((lang, content) -> Utils.stringToBase64(content));

            obj.put("codeOfConducts", codeOfConducts);
            sendResponse(ctx, obj);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler changeCodeOfConduct = ctx -> {
        if(!(server instanceof CodeOfConductFeature)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Minecraft versions lower than 1.21.9 don't support server code-of-conduct.");
            return;
        }

        try {
            final String lang = ctx.queryParam("lang");
            final String content = ctx.bodyAsClass(String.class);
            if(lang == null) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Language is missing.");
                return;
            }

            ((CodeOfConductFeature) server).updateOrCreateCodeOfConduct(lang, !content.isEmpty() ? Utils.base64ToString(content) : "");
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler removeCodeOfConduct = ctx -> {
        if(!(server instanceof CodeOfConductFeature)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Minecraft versions lower than 1.21.9 don't support server code-of-conduct.");
            return;
        }

        try {
            final String lang = ctx.queryParam("lang");
            if(lang == null) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Language is missing.");
                return;
            }

            ((CodeOfConductFeature) server).removeCodeOfConduct(lang);
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler stopServer = ctx -> {
        server.stop();
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler reloadServer = ctx -> {
        server.reload();
        sendResponse(ctx, HttpStatus.OK);
    };

    public Handler switchSave = ctx -> {
        final String saveName = ctx.queryParam("save");
        if(saveName == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Save name is missing.");
            return;
        }

        OPanelSave save = server.getSave(saveName);
        if(save == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the save.");
            return;
        }

        try {
            save.setToCurrent();
            sendResponse(ctx, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };
}
