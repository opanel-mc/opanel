package net.opanel.spigot_1_21_5.command;

import net.opanel.OPanel;
import net.opanel.common.Constants;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class OPanelCommand implements CommandExecutor {
    private final OPanel instance;

    public OPanelCommand(OPanel instance) {
        this.instance = instance;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch(args[0]) {
            case "about" -> sender.sendMessage(Constants.ABOUT_INFO);
            case "status" -> sender.sendMessage(instance.getStatus());
            default -> {
                return false;
            }
        }
        return true;
    }
}
