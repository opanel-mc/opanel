package net.opanel.fabric_helper;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.dedicated.*;
import net.opanel.common.OPanelServer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FabricUtils {
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
}
