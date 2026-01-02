package net.opanel.folia_1_20;

import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FoliaListener implements Listener {
    private final Main plugin;

    public FoliaListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FoliaPlayer(plugin, event.getPlayer())));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new FoliaPlayer(plugin, event.getPlayer())));
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
