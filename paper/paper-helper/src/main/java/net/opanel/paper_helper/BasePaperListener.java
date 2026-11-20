package net.opanel.paper_helper;

import net.opanel.common.OPanelPlayerPosition;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelChunkDirtyEvent;
import net.opanel.event.OPanelPlayerMoveEvent;
import net.opanel.utils.PlayerPositionTracker;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public abstract class BasePaperListener implements Listener {
    protected static final long POSITION_POLL_INTERVAL_TICKS = 1L;

    protected final JavaPlugin plugin;
    private final PlayerPositionTracker positionTracker = new PlayerPositionTracker();
    private final BukkitTask positionPollTask;
    private volatile World cachedMainWorld;

    protected BasePaperListener(JavaPlugin plugin) {
        this(plugin, true);
    }

    protected BasePaperListener(JavaPlugin plugin, boolean pollGlobally) {
        this.plugin = plugin;
        if(pollGlobally) {
            World mainWorld = getMainWorld();
            for(Player player : Bukkit.getOnlinePlayers()) {
                seedPlayerPosition(player, mainWorld);
            }
            positionPollTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::pollOnlinePlayers,
                POSITION_POLL_INTERVAL_TICKS,
                POSITION_POLL_INTERVAL_TICKS
            );
        } else {
            positionPollTask = null;
        }
    }

    protected abstract BasePaperPlayer createPlayer(Player player);

    protected void onPlayerTrackStart(Player player) { }

    protected void onPlayerTrackStop(UUID uuid) { }

    protected void onPositionTrackingStop() { }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinTrack(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        seedPlayerPosition(player, getMainWorld());
        onPlayerTrackStart(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitTrack(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        onPlayerTrackStop(uuid);
        removePlayerPosition(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if(event.getPlugin() != plugin) return;

        if(positionPollTask != null && !positionPollTask.isCancelled()) {
            positionPollTask.cancel();
        }
        onPositionTrackingStop();
        positionTracker.clear();
        cachedMainWorld = null;
    }

    protected void removePlayerPosition(UUID uuid) {
        positionTracker.remove(uuid);
    }

    protected void samplePlayerPosition(Player player) {
        samplePlayerPosition(player, getMainWorld());
    }

    private void pollOnlinePlayers() {
        World mainWorld = getMainWorld();
        for(Player player : Bukkit.getOnlinePlayers()) {
            samplePlayerPosition(player, mainWorld);
        }
    }

    private void seedPlayerPosition(Player player, World mainWorld) {
        Location location = player.getLocation();
        if(location == null) return;

        World world = location.getWorld();
        String save = (world != null && world.equals(mainWorld)) ? world.getName() : null;
        positionTracker.set(
            player.getUniqueId(),
            new OPanelPlayerPosition(location.getX(), location.getY(), location.getZ(), save)
        );
    }

    private void samplePlayerPosition(Player player, World mainWorld) {
        if(!player.isOnline()) return;

        Location location = player.getLocation();
        if(location == null) return;

        World world = location.getWorld();
        String save = (world != null && world.equals(mainWorld)) ? world.getName() : null;
        OPanelPlayerPosition position = positionTracker.track(
            player.getUniqueId(),
            location.getX(),
            location.getY(),
            location.getZ(),
            save
        );
        if(position == null) return;

        EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(createPlayer(player), position));
    }

    private World getMainWorld() {
        World mainWorld = cachedMainWorld;
        if(mainWorld != null) return mainWorld;

        List<World> worlds = plugin.getServer().getWorlds();
        if(worlds.isEmpty()) return null;

        mainWorld = worlds.get(0);
        cachedMainWorld = mainWorld;
        return mainWorld;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        emitDirtyChunk(event.getBlock());
        emitDirtyChunk(event.getToBlock());
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        for(BlockState state : event.getBlocks()) {
            emitDirtyChunk(state.getBlock());
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        emitDirtyChunk(event.getBlock());
        for(Block block : event.blockList()) {
            emitDirtyChunk(block);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for(Block block : event.blockList()) {
            emitDirtyChunk(block);
        }
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        emitDirtyChunk(event.getBlock());
    }

    @EventHandler
    public void onChunkLoad(ChunkPopulateEvent event) {
        World world = event.getWorld();
        if(world.getEnvironment() != World.Environment.NORMAL) return;

        Chunk chunk = event.getChunk();
        EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(chunk.getX(), chunk.getZ()));
    }

    protected static void emitDirtyChunk(Block block) {
        World world = block.getWorld();
        if(world.getEnvironment() != World.Environment.NORMAL) return;

        EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(block.getX() >> 4, block.getZ() >> 4));
    }
}
