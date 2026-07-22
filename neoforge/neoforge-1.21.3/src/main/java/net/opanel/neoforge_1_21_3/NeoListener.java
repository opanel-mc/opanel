package net.opanel.neoforge_1_21_3;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.neoforge_helper.BaseNeoListener;

public class NeoListener extends BaseNeoListener {
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new NeoPlayer((ServerPlayer) event.getEntity())));
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new NeoPlayer((ServerPlayer) event.getEntity())));
    }

    @SubscribeEvent
    public void onPlayerGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        final GameType gamemode = event.getNewGameMode();
        OPanelGameMode opanelGamemode;
        switch(gamemode) {
            case ADVENTURE -> opanelGamemode = OPanelGameMode.ADVENTURE;
            case SURVIVAL -> opanelGamemode = OPanelGameMode.SURVIVAL;
            case CREATIVE -> opanelGamemode = OPanelGameMode.CREATIVE;
            case SPECTATOR -> opanelGamemode = OPanelGameMode.SPECTATOR;
            default -> opanelGamemode = null;
        }
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new NeoPlayer((ServerPlayer) event.getEntity()), opanelGamemode));
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;

        emitPlayerMove(player);
    }

    @SubscribeEvent
    public void onPlayerEnterPortal(EntityTravelToDimensionEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;

        emitPlayerMove(player);
    }

    private static void emitPlayerMove(ServerPlayer player) {
        if(!hasPlayerMoved(player)) return;

        EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(new NeoPlayer(player)));
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        for(BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
            emitDirtyChunk((ServerLevel) event.getLevel(), snapshot.getPos());
        }
    }

    @SubscribeEvent
    public void onBlockExplode(ExplosionEvent.Detonate event) {
        for(BlockPos pos : event.getAffectedBlocks()) {
            emitDirtyChunk((ServerLevel) event.getLevel(), pos);
        }
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onCreateFluidSource(CreateFluidSourceEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onCropGrow(CropGrowEvent.Post event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onSaplingGrowTree(BlockGrowFeatureEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        ServerLevel world = (ServerLevel) event.getLevel();
        if(world.dimension() != Level.OVERWORLD) return;

        ChunkPos pos = event.getChunk().getPos();
        EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.x, pos.z));
    }

    private static void emitDirtyChunk(ServerLevel world, BlockPos pos) {
        if(world.dimension() != Level.OVERWORLD) return;

        EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.getX() >> 4, pos.getZ() >> 4));
    }
}
