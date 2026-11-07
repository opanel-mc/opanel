package net.opanel.paper_helper;

import net.opanel.common.OPanelPlayer;
import net.opanel.event.OPanelPlayerMoveEvent;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelChunkDirtyEvent;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;

public abstract class BasePaperListener implements Listener {
    protected abstract OPanelPlayer createPlayer(Player player);

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMovePosition(PlayerMoveEvent event) {
        Location to = event.getTo();
        if(to != null && (hasPlayerMoved(event) || event.getFrom().getWorld() != to.getWorld())) {
            updatePosition(event.getPlayer(), to);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportPosition(PlayerTeleportEvent event) {
        updatePosition(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalPosition(PlayerPortalEvent event) {
        updatePosition(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawnPosition(PlayerRespawnEvent event) {
        updatePosition(event.getPlayer(), event.getRespawnLocation());
    }

    private void updatePosition(Player player, Location location) {
        if(location == null) return;

        EventManager.get().emit(EventType.PLAYER_POSITION_UPDATE,
                new OPanelPlayerMoveEvent(createPlayer(player), BasePaperPlayer.serializePosition(location)));
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

    protected static boolean hasPlayerMoved(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ());
    }
}
