package net.opanel.folia_26_1_2;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.opanel.paper_helper.BasePaperListener;
import net.opanel.paper_helper.BasePaperPlayer;
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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class FoliaListener extends BasePaperListener implements Listener {
    private final Main plugin;
    private final ConcurrentHashMap<UUID, ScheduledTask> playerPositionTasks = new ConcurrentHashMap<>();

    public FoliaListener(Main plugin) {
        super(plugin, false);
        this.plugin = plugin;

        for(Player player : Bukkit.getOnlinePlayers()) {
            onPlayerTrackStart(player);
        }
    }

    @Override
    protected BasePaperPlayer createPlayer(Player player) {
        return new FoliaPlayer(plugin, player);
    }

    @Override
    protected void onPlayerTrackStart(Player player) {
        UUID uuid = player.getUniqueId();
        onPlayerTrackStop(uuid);

        final AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
        ScheduledTask task = player.getScheduler().runAtFixedRate(
            plugin,
            scheduledTask -> samplePlayerPosition(player),
            () -> {
                ScheduledTask current = taskRef.get();
                if(current != null && playerPositionTasks.remove(uuid, current)) {
                    removePlayerPosition(uuid);
                }
            },
            POSITION_POLL_INTERVAL_TICKS,
            POSITION_POLL_INTERVAL_TICKS
        );
        if(task != null) {
            playerPositionTasks.put(uuid, task);
            taskRef.set(task);
            if(task.isCancelled() && playerPositionTasks.remove(uuid, task)) {
                removePlayerPosition(uuid);
            }
        }
    }

    @Override
    protected void onPlayerTrackStop(UUID uuid) {
        ScheduledTask task = playerPositionTasks.remove(uuid);
        if(task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    @Override
    protected void onPositionTrackingStop() {
        for(ScheduledTask task : playerPositionTasks.values()) {
            if(!task.isCancelled()) {
                task.cancel();
            }
        }
        playerPositionTasks.clear();
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
