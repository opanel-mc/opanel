package net.opanel.fabric_helper.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.opanel.common.OPanelDimension;
import net.opanel.common.OPanelServer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelChunkDirtyEvent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FabricUtils {
    public static void setRuntimeHardcore(MinecraftDedicatedServer server, boolean enabled) throws IOException {
        // Update only the cached value; loading server.properties could apply another save's settings.
        String fieldName = FabricLoader.getInstance().getMappingResolver()
                .mapFieldName("intermediary", "net.minecraft.class_3806", "field_16838", "Z");
        try {
            Field hardcoreField = ServerPropertiesHandler.class.getDeclaredField(fieldName);
            hardcoreField.setAccessible(true);
            hardcoreField.setBoolean(server.getProperties(), enabled);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Cannot update runtime hardcore setting", e);
        }
    }

    public static boolean forceUpdateProperties(MinecraftDedicatedServer server) {
        try {
            Field propertiesLoaderField = MinecraftDedicatedServer.class.getDeclaredField("propertiesLoader");
            propertiesLoaderField.setAccessible(true);
            ServerPropertiesLoader propertiesLoader = (ServerPropertiesLoader) propertiesLoaderField.get(server);
            propertiesLoader.apply(h -> ServerPropertiesHandler.load(OPanelServer.serverPropertiesPath));
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return true;
    }

    public static void addCompoundToNBTList(NbtList list, NbtCompound compound, int index) {
        if(index < 0) throw new IllegalArgumentException("Target index is out of the list size.");
        if(index >= list.size()) {
            list.add(compound);
            return;
        }

        List<NbtElement> tempList = new ArrayList<>();
        for(int i = index; i < list.size(); i++) {
            tempList.add(list.remove(i));
            i--;
        }
        list.add(compound);
        list.addAll(tempList);
    }

    public static void emitDirtyChunk(World world, BlockPos pos) {
        if(world.getRegistryKey() != World.OVERWORLD) return;

        EventManager.get().emit(EventType.CHUNK_DIRTY, new OPanelChunkDirtyEvent(pos.getX() >> 4, pos.getZ() >> 4));
    }

    /**
     * @return the server world of the dimension (overworld by default)
     */
    public static ServerWorld getWorldByDimension(MinecraftServer server, OPanelDimension dimension) {
        switch(dimension) {
            case OVERWORLD -> { return server.getOverworld(); }
            case NETHER -> { return server.getWorld(ServerWorld.NETHER); }
            case THE_END -> { return server.getWorld(ServerWorld.END); }
        }
        return server.getOverworld();
    }
}
