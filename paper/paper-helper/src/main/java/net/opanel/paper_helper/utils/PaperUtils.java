package net.opanel.paper_helper.utils;

import com.mojang.brigadier.CommandDispatcher;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import net.opanel.common.OPanelDimension;
import net.opanel.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class PaperUtils {
    private static final boolean leaves = Utils.hasClass("org.leavesmc.leaves.LeavesConfig");

    public static Object getDedicatedServer() throws ReflectiveOperationException {
        Server craftServer = Bukkit.getServer();
        return craftServer.getClass().getMethod("getServer").invoke(craftServer);
    }

    /** Not compatible with <= 1.16.5 */
    public static CommandDispatcher<?> getCommandDispatcher(boolean obf) throws ReflectiveOperationException {
        Object dedicatedServer = getDedicatedServer();
        Object manager = dedicatedServer.getClass().getMethod(obf ? "aC" : "getCommands").invoke(dedicatedServer); // aC -> getCommands
        return (CommandDispatcher<?>) manager.getClass().getMethod(obf ? "a" : "getDispatcher").invoke(manager); // a -> getDispatcher
    }

    /** Not compatible with <= 1.16.5 */
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

    /** Not compatible with <= 1.16.5 */
    public static int getMinY(Server server) {
        return getMinY(server.getWorlds().get(0));
    }

    /** Not compatible with <= 1.16.5 */
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
