package net.opanel.forge_1_21_11;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.forge_helper.InventorySerializer;
import net.opanel.forge_helper.InventorySyncTask;

import java.util.Map;

public class ForgeListener {
    private InventorySyncTask inventorySyncTask;
    private MinecraftServer server;

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Store server reference if we don't have one
        if (server == null) server = player.getServer();
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new ForgePlayer(player, server)));
        if (inventorySyncTask != null) inventorySyncTask.syncPlayer(player);
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new ForgePlayer(player, server)));
        if (inventorySyncTask != null) inventorySyncTask.removePlayer(player.getStringUUID());
    }

    @SubscribeEvent
    public void onPlayerGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        final GameType gamemode = event.getNewGameMode();
        OPanelGameMode opanelGamemode = switch(gamemode) {
            case ADVENTURE -> OPanelGameMode.ADVENTURE;
            case SURVIVAL -> OPanelGameMode.SURVIVAL;
            case CREATIVE -> OPanelGameMode.CREATIVE;
            case SPECTATOR -> OPanelGameMode.SPECTATOR;
            default -> null;
        };
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new ForgePlayer(player, server), opanelGamemode));
    }

    public void setInventorySyncTask(InventorySyncTask task) { this.inventorySyncTask = task; }

    public void emitInventoryChange(ServerPlayer player) {
        if (player == null) return;
        Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
        String hash = InventorySerializer.generateInventoryHash(player);
        if (inventorySyncTask != null) inventorySyncTask.updateHash(player.getStringUUID(), hash);
        EventManager.get().emit(EventType.PLAYER_INVENTORY_CHANGE, new OPanelPlayerInventoryChangeEvent(new ForgePlayer(player, server), inventoryData));
    }
}
