package net.opanel.paper_helper.utils;

import com.mojang.brigadier.CommandDispatcher;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import net.opanel.common.OPanelDimension;
import net.opanel.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PaperUtils {
    private static final boolean leaves = Utils.hasClass("org.leavesmc.leaves.LeavesConfig");

    private static Object getWorldData(World world) throws ReflectiveOperationException {
        Object handle = world.getClass().getMethod("getHandle").invoke(world);
        // Paper 1.20.5+ uses Mojang names; older releases use Spigot/obfuscated names.
        // getLevelData: 1.19.4 = n_, 1.20/1.20.1 = u_, 1.20.2 = z_, 1.20.3/1.20.4 = B_.
        for(String name : new String[] {"getLevelData", "getWorldData", "n_", "u_", "z_", "B_"}) {
            try {
                Method method = handle.getClass().getMethod(name);
                if(method.getReturnType().getPackageName().equals("net.minecraft.world.level.storage")) {
                    return method.invoke(handle);
                }
            } catch (NoSuchMethodException e) {
                // Try the mapping used by another supported server version.
            }
        }
        throw new NoSuchMethodException("Cannot find the running world's level data");
    }

    public static boolean isDifficultyLocked(World world) throws IOException {
        try {
            Object worldData = getWorldData(world);
            Method method;
            try {
                method = worldData.getClass().getMethod("isDifficultyLocked");
            } catch (NoSuchMethodException e) {
                method = worldData.getClass().getMethod("t"); // 1.19.4 - 1.20.4
            }
            return (boolean) method.invoke(worldData);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Cannot read runtime difficulty lock", e);
        }
    }

    public static void setDifficultyLocked(World world, boolean locked) throws IOException {
        try {
            Object worldData = getWorldData(world);
            Method method;
            try {
                method = worldData.getClass().getMethod("setDifficultyLocked", boolean.class);
            } catch (NoSuchMethodException e) {
                method = worldData.getClass().getMethod("d", boolean.class); // 1.19.4 - 1.20.4
            }
            method.invoke(worldData, locked);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Cannot update runtime difficulty lock", e);
        }
    }

    public static Object getDedicatedServer() throws ReflectiveOperationException {
        Server craftServer = Bukkit.getServer();
        return craftServer.getClass().getMethod("getServer").invoke(craftServer);
    }

    public static CommandDispatcher<?> getCommandDispatcher(boolean obf) throws ReflectiveOperationException {
        Object dedicatedServer = getDedicatedServer();
        Object manager = dedicatedServer.getClass().getMethod(obf ? "aC" : "getCommands").invoke(dedicatedServer); // aC -> getCommands
        return (CommandDispatcher<?>) manager.getClass().getMethod(obf ? "a" : "getDispatcher").invoke(manager); // a -> getDispatcher
    }

    public static void performCommand(String command, boolean obf) throws ReflectiveOperationException {
        Object dedicatedServer = PaperUtils.getDedicatedServer();
        Object manager = dedicatedServer.getClass().getMethod(obf ? "aC" : "getCommands").invoke(dedicatedServer); // aC -> getCommands
        Object source = dedicatedServer.getClass().getMethod(obf ? "aD" : "createCommandSourceStack").invoke(dedicatedServer); // aD -> createCommandSourceStack
        manager.getClass().getMethod(obf ? "a" : "performPrefixedCommand", source.getClass(), String.class).invoke(manager, source, command); // a -> performPrefixedCommand
    }

    public static void addCompoundToNBTList(ReadWriteNBTCompoundList list, ReadWriteNBT compound, int index) {
        if(index < 0) throw new IllegalArgumentException("Target index is out of the list size.");
        if(index >= list.size()) {
            list.addCompound(compound);
            return;
        }

        List<ReadWriteNBT> tempList = new ArrayList<>();
        for(int i = index; i < list.size(); i++) {
            tempList.add(list.remove(i));
            i--;
        }
        list.addCompound(compound);
        for(ReadWriteNBT item : tempList) {
            list.addCompound(item);
        }
    }

    /**
     * @return the world of the dimension (overworld by default)
     */
    public static World getWorldByDimension(OPanelDimension dimension) {
        for(World world : Bukkit.getWorlds()) {
            if(
                (world.getEnvironment() == World.Environment.NORMAL && dimension == OPanelDimension.OVERWORLD)
                || (world.getEnvironment() == World.Environment.NETHER && dimension == OPanelDimension.NETHER)
                || (world.getEnvironment() == World.Environment.THE_END && dimension == OPanelDimension.THE_END)
            ) {
                return world;
            }
        }
        return Bukkit.getWorlds().get(0);
    }

    public static int getMinY(Server server) {
        return getMinY(server.getWorlds().get(0));
    }

    public static int getMinY(World world) {
        try {
            return (int) world.getClass().getMethod("getMinHeight").invoke(world);
        } catch (ReflectiveOperationException e) {
            return -64;
        }
    }

    public static int getMaxY(Server server) {
        return getMaxY(server.getWorlds().get(0));
    }

    public static int getMaxY(World world) {
        return world.getMaxHeight();
    }

    public static boolean isLeaves() {
        return leaves;
    }
}
