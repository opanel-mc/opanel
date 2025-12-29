package net.opanel.bukkit_helper;

import com.mojang.brigadier.CommandDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.Server;

public class BukkitUtils {
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
        Object dedicatedServer = BukkitUtils.getDedicatedServer();
        Object manager = dedicatedServer.getClass().getMethod(obf ? "aC" : "getCommands").invoke(dedicatedServer); // aC -> getCommands
        Object source = dedicatedServer.getClass().getMethod(obf ? "aD" : "createCommandSourceStack").invoke(dedicatedServer); // aD -> createCommandSourceStack
        manager.getClass().getMethod(obf ? "a" : "performPrefixedCommand", source.getClass(), String.class).invoke(manager, source, command); // a -> performPrefixedCommand
    }
}
