package net.opanel.forge_1_21_3;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.opanel.common.OPanelGameMode;
import net.opanel.event.*;
import net.opanel.forge_helper.BaseForgeListener;

import static net.opanel.forge_helper.utils.ForgeUtils.emitDirtyChunk;

public class ForgeListener extends BaseForgeListener {
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        EventManager.get().emit(EventType.PLAYER_JOIN, new OPanelPlayerJoinEvent(new ForgePlayer((ServerPlayer) event.getEntity())));
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        EventManager.get().emit(EventType.PLAYER_LEAVE, new OPanelPlayerLeaveEvent(new ForgePlayer((ServerPlayer) event.getEntity())));
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
        EventManager.get().emit(EventType.PLAYER_GAMEMODE_CHANGE, new OPanelPlayerGameModeChangeEvent(new ForgePlayer((ServerPlayer) event.getEntity()), opanelGamemode));
    }

    @SubscribeEvent
    public void onPlayerMove(TickEvent.PlayerTickEvent.Post event) {
        if(!(event.player instanceof ServerPlayer player)) return;
        
        emitPlayerMove(player);
    }

    @SubscribeEvent
    public void onPlayerEnterPortal(EntityTravelToDimensionEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;

        emitPlayerMove(player);
    }

    private static void emitPlayerMove(ServerPlayer player) {
        if(!hasPlayerMoved(player)) return;

        EventManager.get().emit(EventType.PLAYER_MOVE, new OPanelPlayerMoveEvent(new ForgePlayer(player)));
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
    public void onCreateFluidSource(BlockEvent.CreateFluidSourceEvent event) {
        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onCropGrow(BlockEvent.CropGrowEvent.Post event) {
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

//    @SubscribeEvent
//    public void onSaplingGrowTree(SaplingGrowTreeEvent event) {
//        emitDirtyChunk((ServerLevel) event.getLevel(), event.getPos());
//    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        ServerLevel world = (ServerLevel) event.getLevel();
        if(world.dimension() != Level.OVERWORLD) return;

        ChunkPos pos = event.getChunk().getPos();
        EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.x, pos.z));
    }
}
