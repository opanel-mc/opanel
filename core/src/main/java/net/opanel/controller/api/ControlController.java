package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import net.opanel.common.features.BukkitConfigFeature;
import net.opanel.common.features.CodeOfConductFeature;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;
import net.opanel.controller.BaseController;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
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
            final String properties = ctx.body();
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
            if(lang == null) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Language is missing.");
                return;
            }

            final String content = ctx.body();
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

    public Handler restartServer = ctx -> {
        final String launchCommand = Storage.get().getStoredData(StorageKey.LAUNCH_COMMAND);
        if(launchCommand == null || launchCommand.isEmpty()) {
            sendResponse(ctx, HttpStatus.NOT_ACCEPTABLE, "Launch command is not set.");
            return;
        }
        server.restart();
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

    public Handler getBukkitServerConfig = ctx -> {
        if(!(server instanceof BukkitConfigFeature)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "This server is not a bukkit server.");
            return;
        }

        ServerType serverType = server.getServerType();
        try {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("bukkit", Utils.stringToBase64(((BukkitConfigFeature) server).getBukkitServerConfigContent("bukkit")));
            if(serverType == ServerType.SPIGOT || serverType == ServerType.PAPER || serverType == ServerType.FOLIA || serverType == ServerType.LEAVES) {
                obj.put("spigot", Utils.stringToBase64(
                    ((BukkitConfigFeature) server).getBukkitServerConfigContent("spigot")
                ));
            }
            if(serverType == ServerType.PAPER || serverType == ServerType.FOLIA || serverType == ServerType.LEAVES) {
                obj.put("paper", Utils.stringToBase64(
                    ((BukkitConfigFeature) server).getBukkitServerConfigContent("paper")
                ));
            }
            if(serverType == ServerType.LEAVES) {
                obj.put("leaves", Utils.stringToBase64(
                    ((BukkitConfigFeature) server).getBukkitServerConfigContent("leaves")
                ));
            }
            sendResponse(ctx, obj);
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Unknown target.");
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the target bukkit server config.");
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler setBukkitServerConfig = ctx -> {
        if(!(server instanceof BukkitConfigFeature)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "This server is not a bukkit server.");
            return;
        }

        try {
            final String target = ctx.queryParam("target");
            if(target == null) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Target is missing.");
                return;
            }

            final String content = ctx.body();
            if(content.isEmpty()) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Config content is missing.");
                return;
            }

            ((BukkitConfigFeature) server).writeBukkitServerConfigContent(target, Utils.base64ToString(content));
            sendResponse(ctx, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Unknown target.");
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the target bukkit server config.");
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler getPaperWorldConfig = ctx -> {
        if(!(server instanceof BukkitConfigFeature)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "This server is not a bukkit server.");
            return;
        }

        try {
            final String worldName = ctx.queryParam("world");

            HashMap<String, Object> obj = new HashMap<>();
            if(worldName == null) {
                obj.put("config", ((BukkitConfigFeature) server).getPaperWorldDefaultsConfigContent());
            } else {
                obj.put("config", ((BukkitConfigFeature) server).getPaperWorldConfigContent(worldName));
            }
            sendResponse(ctx, obj);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the world config.");
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler setPaperWorldConfig = ctx -> {
        if(!(server instanceof BukkitConfigFeature)) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, "This server is not a bukkit server.");
            return;
        }

        try {
            final String worldName = ctx.queryParam("world");
            final String content = ctx.body();
            if(content.isEmpty()) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Config content is missing.");
            }

            if(worldName == null) {
                ((BukkitConfigFeature) server).writePaperWorldDefaultsConfigContent(Utils.base64ToString(content));
            } else {
                ((BukkitConfigFeature) server).writePaperWorldConfigContent(worldName, Utils.base64ToString(content));
            }
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchFileException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Cannot find the world config.");
        } catch (IOException e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler getLaunchCommand = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("launchCommand", Storage.get().getStoredData(StorageKey.LAUNCH_COMMAND));
        sendResponse(ctx, obj);
    };

    public Handler setLaunchCommand = ctx -> {
        try {
            final String launchCommand = ctx.body();
            if(launchCommand.isEmpty()) {
                sendResponse(ctx, HttpStatus.BAD_REQUEST, "Launch command is missing.");
                return;
            }

            Storage.get().setStoredData(StorageKey.LAUNCH_COMMAND, launchCommand);
            sendResponse(ctx, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };
}
