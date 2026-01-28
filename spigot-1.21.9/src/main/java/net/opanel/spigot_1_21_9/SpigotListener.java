package net.opanel.spigot_1_21_9;

import net.opanel.bukkit_helper.InventorySerializer;
import net.opanel.bukkit_helper.InventorySyncTask;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.Map;

public class SpigotListener implements Listener {
    private final Main plugin;
    private InventorySyncTask inventorySyncTask;

    public SpigotListener(Main plugin) {
        this.plugin = plugin;
    }

    public void setInventorySyncTask(InventorySyncTask task) {
        this.inventorySyncTask = task;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new SpigotPlayer(plugin, event.getPlayer())));
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (inventorySyncTask != null) {
                inventorySyncTask.syncPlayer(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new SpigotPlayer(plugin, event.getPlayer())));
        
        if (inventorySyncTask != null) {
            inventorySyncTask.removePlayer(event.getPlayer().getUniqueId().toString());
        }
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        final GameMode gamemode = event.getNewGameMode();
        OPanelGameMode opanelGamemode;
        switch(gamemode) {
            case ADVENTURE -> opanelGamemode = OPanelGameMode.ADVENTURE;
            case SURVIVAL -> opanelGamemode = OPanelGameMode.SURVIVAL;
            case CREATIVE -> opanelGamemode = OPanelGameMode.CREATIVE;
            case SPECTATOR -> opanelGamemode = OPanelGameMode.SPECTATOR;
            default -> opanelGamemode = null;
        }
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new SpigotPlayer(plugin, event.getPlayer()), opanelGamemode));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            emitInventoryChange(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            emitInventoryChange(event.getPlayer());
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            emitInventoryChange(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            emitInventoryChange(event.getPlayer());
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            emitInventoryChange(event.getPlayer());
        }, 1L);
    }

    private void emitInventoryChange(Player player) {
        if (player == null || !player.isOnline()) return;
        
        try {
            String uuid = player.getUniqueId().toString();
            Map<String, Object> inventoryData = InventorySerializer.serializeInventory(player);
            String hash = InventorySerializer.generateInventoryHash(player);
            
            if (inventorySyncTask != null) {
                inventorySyncTask.updateHash(uuid, hash);
            }
            
            SpigotPlayer opanelPlayer = new SpigotPlayer(plugin, player);
            EventManager.get().emit(
                EventType.PLAYER_INVENTORY_CHANGE,
                new OPanelPlayerInventoryChangeEvent(opanelPlayer, inventoryData)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
