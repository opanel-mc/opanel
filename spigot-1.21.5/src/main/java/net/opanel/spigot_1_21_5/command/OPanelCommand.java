package net.opanel.spigot_1_21_5.command;

import net.opanel.OPanel;
import net.opanel.common.Constants;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OPanelCommand implements CommandExecutor, TabCompleter {
    private final OPanel instance;

    public OPanelCommand(OPanel instance) {
        this.instance = instance;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length != 1) return false;
        switch(args[0]) {
            case "about" -> sender.sendMessage(Constants.ABOUT_INFO);
            case "status" -> sender.sendMessage(instance.getStatus());
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length != 1) return List.of();
        return List.of("about", "status");
    }
}
