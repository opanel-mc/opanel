package net.opanel.paper_1_21_9;

import net.opanel.paper_helper.BasePaperListener;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PaperListener extends BasePaperListener implements Listener {
    private final Main plugin;

    public PaperListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new PaperPlayer(plugin, event.getPlayer())));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new PaperPlayer(plugin, event.getPlayer())));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!hasPlayerMoved(event)) return;

        PaperPlayer player = new PaperPlayer(plugin, event.getPlayer());
        Location position = event.isCancelled() ? event.getFrom() : event.getTo();
        EventManager.get().emit(EventType.PLAYER_MOVE,
                new OPanelPlayerMoveEvent(player, PaperPlayer.serializePosition(position)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        updatePosition(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        onPlayerMove(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalPosition(PlayerPortalEvent event) {
        updatePosition(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        updatePosition(event.getPlayer(), event.getRespawnLocation());
    }

    private void updatePosition(Player player, Location location) {
        if(location == null) return;

        EventManager.get().emit(EventType.PLAYER_POSITION_UPDATE, new OPanelPlayerMoveEvent(
                new PaperPlayer(plugin, player), PaperPlayer.serializePosition(location)));
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
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new PaperPlayer(plugin, event.getPlayer()), opanelGamemode));
    }
}
