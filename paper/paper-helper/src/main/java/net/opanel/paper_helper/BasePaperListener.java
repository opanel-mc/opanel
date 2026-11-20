package net.opanel.paper_helper;

import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelChunkDirtyEvent;
import net.opanel.event.OPanelPlayerMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public abstract class BasePaperListener implements Listener {
    protected final JavaPlugin plugin;

    protected BasePaperListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    protected abstract BasePaperPlayer createPlayer(Player player);

    protected void runAfterPlayerAction(Player player, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if(hasLocationMoved(event.getFrom(), to)) {
            emitPlayerMove(event.getPlayer(), to);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleMove(VehicleMoveEvent event) {
        List<Entity> passengers = event.getVehicle().getPassengers();
        if(passengers.isEmpty()) return;

        Location to = event.getTo();
        if(!hasLocationMoved(event.getFrom(), to)) return;

        for(Entity passenger : passengers) {
            if(passenger instanceof Player player) {
                emitPlayerMove(player, to);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        emitCompletedPlayerMove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        emitCompletedPlayerMove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        emitCompletedPlayerMove(event.getPlayer());
    }

    private void emitCompletedPlayerMove(Player player) {
        runAfterPlayerAction(player, () -> {
            if(player.isOnline()) emitPlayerMove(player, player.getLocation());
        });
    }

    private void emitPlayerMove(Player player, Location location) {
        if(location == null) return;

        BasePaperPlayer wrapped = createPlayer(player);
        EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(wrapped, wrapped.toPosition(location)));
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

    protected static boolean hasLocationMoved(Location from, Location to) {
        if(to == null) return false;
        if(from == null) return true;
        return from.getWorld() != to.getWorld()
                || from.getX() != to.getX()
                || from.getY() != to.getY()
                || from.getZ() != to.getZ();
    }
}
