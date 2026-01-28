package net.opanel.fabric_1_20_2;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.fabric_helper.InventorySerializer;
import net.opanel.fabric_helper.InventorySyncTask;
import net.opanel.fabric_helper.event.PlayerGameModeChangeEvent;

import java.util.Map;

public class FabricListener {
    private InventorySyncTask inventorySyncTask;

    public FabricListener() {
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> {
            ServerPlayerEntity player = networkHandler.getPlayer();
            EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FabricPlayer(player)));
            server.execute(() -> {
                if (inventorySyncTask != null && player != null) inventorySyncTask.syncPlayer(player);
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) -> {
            ServerPlayerEntity player = networkHandler.getPlayer();
            EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new FabricPlayer(player)));
            if (inventorySyncTask != null && player != null) inventorySyncTask.removePlayer(player.getUuidAsString());
        });

        PlayerGameModeChangeEvent.EVENT.register(((player, gamemode) -> {
            OPanelGameMode opanelGamemode = switch(gamemode) {
                case ADVENTURE -> OPanelGameMode.ADVENTURE;
                case SURVIVAL -> OPanelGameMode.SURVIVAL;
                case CREATIVE -> OPanelGameMode.CREATIVE;
                case SPECTATOR -> OPanelGameMode.SPECTATOR;
            };
            EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new FabricPlayer(player), opanelGamemode));
        }));
    }

    public void setInventorySyncTask(InventorySyncTask task) { this.inventorySyncTask = task; }

    public void emitInventoryChange(ServerPlayerEntity player) {
        if (player == null) return;
        String uuid = player.getUuidAsString();
        Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
        String hash = InventorySerializer.generateInventoryHash(player);
        if (inventorySyncTask != null) inventorySyncTask.updateHash(uuid, hash);
        EventManager.get().emit(EventType.PLAYER_INVENTORY_CHANGE, new OPanelPlayerInventoryChangeEvent(new FabricPlayer(player), inventoryData));
    }
}
