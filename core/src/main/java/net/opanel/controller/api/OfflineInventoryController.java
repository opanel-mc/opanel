package net.opanel.controller.api;

import com.google.gson.reflect.TypeToken;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelPlayer;
import net.opanel.controller.BaseController;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

public class OfflineInventoryController extends BaseController {
    private static final Type inventoryType = new TypeToken<List<OPanelInventory.OPanelItemStack>>() {}.getType();

    public OfflineInventoryController(OPanel plugin) {
        super(plugin);
    }

    public Handler getOfflineInventory = ctx -> {
        String playerName = ctx.pathParam("playerName");
        OPanelPlayer player = getPlayerByName(playerName);
        if(player == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Player not found.");
            return;
        }

        if(player.isOnline()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Player is online.");
            return;
        }

        OPanelInventory inventory = player.getInventory();
        if(inventory == null) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Inventory unavailable.");
            return;
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("name", player.getName());
        obj.put("uuid", player.getUUID());
        obj.put("size", inventory.getSize());
        obj.put("items", inventory.getItems());
        sendResponse(ctx, obj);
    };

    public Handler updateOfflineInventory = ctx -> {
        String playerName = ctx.pathParam("playerName");
        OPanelPlayer player = getPlayerByName(playerName);
        if(player == null) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Player not found.");
            return;
        }

        if(player.isOnline()) {
            sendResponse(ctx, HttpStatus.FORBIDDEN, "Player is online.");
            return;
        }

        List<OPanelInventory.OPanelItemStack> items = ctx.bodyAsClass(inventoryType);
        if(items == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Invalid inventory payload.");
            return;
        }

        OPanelInventory inventory = player.getInventory();
        if(inventory == null) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Inventory unavailable.");
            return;
        }

        inventory.setItems(items);
        sendResponse(ctx, HttpStatus.OK);
    };

    private OPanelPlayer getPlayerByName(String playerName) {
        if(playerName == null || playerName.isEmpty()) return null;
        for(OPanelPlayer player : server.getPlayers()) {
            if(playerName.equalsIgnoreCase(player.getName())) return player;
        }
        return null;
    }
}