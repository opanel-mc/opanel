package net.opanel.folia_1_20.command;

import net.opanel.OPanel;
import net.opanel.common.Constants;
import net.opanel.web.WebServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class OPanelCommand implements CommandExecutor, TabCompleter {
    private final OPanel instance;

    public OPanelCommand(OPanel instance) {
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length != 1) return false;
        switch(args[0]) {
            case "about" -> sender.sendMessage(Constants.ABOUT_INFO);
            case "status" -> sender.sendMessage(instance.getStatus());
            case "start" -> {
                WebServer webServer = instance.getWebServer();
                if(webServer.isRunning()) {
                    sender.sendMessage("Web panel is already started.");
                } else {
                    try {
                        webServer.start();
                        sender.sendMessage("Web panel is started successfully.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            case "stop" -> {
                WebServer webServer = instance.getWebServer();
                if(!webServer.isRunning()) {
                    sender.sendMessage("Web panel is already stopped.");
                } else {
                    try {
                        webServer.stop();
                        sender.sendMessage("Web panel is stopped successfully.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length != 1) return List.of();
        return List.of("about", "status", "start", "stop");
    }
}