package net.opanel.folia_1_20_5;

import net.opanel.paper_helper.BasePaperListener;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.*;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FoliaListener extends BasePaperListener implements Listener {
    private final Main plugin;

    public FoliaListener(Main plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected boolean usesScheduledPositions() {
        return true;
    }

    @Override
    protected OPanelPlayer createPlayer(Player player) {
        return new FoliaPlayer(plugin, player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runAtFixedRate(plugin, task -> samplePosition(player, task::cancel),
                null, 1L, POSITION_SAMPLE_INTERVAL);
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new FoliaPlayer(plugin, event.getPlayer())));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        clearPosition(event.getPlayer());
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new FoliaPlayer(plugin, event.getPlayer())));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!hasPlayerMoved(event)) return;

        EventManager.get().emit(EventType.PLAYER_MOVE, OPanelPlayerMoveEvent.forExtensions(new FoliaPlayer(plugin, event.getPlayer())));
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        onPlayerMove(event);
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
