package net.opanel.folia_1_21;

import net.opanel.bukkit_helper.InventorySerializer;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class FoliaListener implements Listener {
    private final Main plugin;
    private FoliaInventorySyncTask inventorySyncTask;

    public FoliaListener(Main plugin) {
        this.plugin = plugin;
    }

    public void setInventorySyncTask(FoliaInventorySyncTask task) {
        this.inventorySyncTask = task;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FoliaPlayer(plugin, player)));
        
        if (inventorySyncTask != null) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
                inventorySyncTask.syncPlayer(player);
            }, 20L);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new FoliaPlayer(plugin, player)));
        
        if (inventorySyncTask != null) {
            inventorySyncTask.removePlayer(player.getUniqueId().toString());
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
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new FoliaPlayer(plugin, event.getPlayer()), opanelGamemode));
    }
}

