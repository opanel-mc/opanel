package net.opanel.forge_helper.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.opanel.common.OPanelDimension;
import net.opanel.common.OPanelServer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelChunkDirtyEvent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ForgeUtils {
    public static void setRuntimeHardcore(DedicatedServer server, boolean enabled, boolean obf) throws IOException {
        // Update only the cached value; loading server.properties could apply another save's settings.
        try {
            Field hardcoreField = DedicatedServerProperties.class.getDeclaredField(obf ? "f_139752_" : "hardcore");
            hardcoreField.setAccessible(true);
            hardcoreField.setBoolean(server.getProperties(), enabled);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Cannot update runtime hardcore setting", e);
        }
    }

    public static boolean forceUpdateProperties(DedicatedServer server, boolean obf) {
        try {
            Field serverSettingsField = DedicatedServer.class.getDeclaredField(obf ? "f_139604_" : "settings"); // f_139604_ -> settings
            serverSettingsField.setAccessible(true);
            DedicatedServerSettings serverSettings = (DedicatedServerSettings) serverSettingsField.get(server);
            serverSettings.update(p -> DedicatedServerProperties.fromFile(OPanelServer.serverPropertiesPath));
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return true;
    }

    public static void addCompoundToNBTList(ListTag list, CompoundTag compound, int index) {
        if(index < 0) throw new IllegalArgumentException("Target index is out of the list size.");
        if(index >= list.size()) {
            list.add(compound);
            return;
        }

        List<Tag> tempList = new ArrayList<>();
        for(int i = index; i < list.size(); i++) {
            tempList.add(list.remove(i));
            i--;
        }
        list.add(compound);
        list.addAll(tempList);
    }

    public static void emitDirtyChunk(ServerLevel world, BlockPos pos) {
        if(world.dimension() != Level.OVERWORLD) return;

        EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.getX() >> 4, pos.getZ() >> 4));
    }

    /**
     * @return the server level of the dimension (overworld by default)
     */
    public static ServerLevel getLevelByDimension(MinecraftServer server, OPanelDimension dimension) {
        switch(dimension) {
            case OVERWORLD -> { return server.overworld(); }
            case NETHER -> { return server.getLevel(ServerLevel.NETHER); }
            case THE_END -> { return server.getLevel(ServerLevel.END); }
        }
        return server.overworld();
    }
}
