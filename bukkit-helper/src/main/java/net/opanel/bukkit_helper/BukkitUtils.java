package net.opanel.bukkit_helper;

import com.mojang.brigadier.CommandDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.Server;

public class BukkitUtils {
    public static Object getDedicatedServer() throws ReflectiveOperationException {
        Server craftServer = Bukkit.getServer();
        return craftServer.getClass().getMethod("getServer").invoke(craftServer);
    }

    public static CommandDispatcher<?> getCommandDispatcher(boolean obf) throws ReflectiveOperationException {
        Object dedicatedServer = getDedicatedServer();
        Object commands = dedicatedServer.getClass().getMethod(obf ? "aC" : "getCommands").invoke(dedicatedServer); // aC -> getCommands
        return (CommandDispatcher<?>) commands.getClass().getMethod(obf ? "a" : "getDispatcher").invoke(commands); // a -> getDispatcher
    }
}
