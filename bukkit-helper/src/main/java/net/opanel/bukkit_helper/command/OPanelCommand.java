package net.opanel.bukkit_helper.command;

import net.opanel.OPanel;
import net.opanel.backup.BackupManager;
import net.opanel.backup.BackupResult;
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
        if (args.length != 1)
            return false;
        switch (args[0]) {
            case "about" -> sender.sendMessage(Constants.ABOUT_INFO);
            case "status" -> sender.sendMessage(instance.getStatus());
            case "start" -> {
                WebServer webServer = instance.getWebServer();
                if (webServer.isRunning()) {
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
                if (!webServer.isRunning()) {
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
            case "backup" -> {
                BackupManager backupManager = instance.getBackupManager();
                if (backupManager == null || !backupManager.isConfigured()) {
                    sender.sendMessage("§cBackup is not configured. Please configure backup settings first.");
                } else {
                    sender.sendMessage("§aStarting backup...");
                    backupManager.performBackupAsync(() -> {
                        // saveAll runs on the calling thread (which should be main thread)
                        instance.getServer().saveAll();
                    }).thenAccept(result -> {
                        if (result.isSuccess()) {
                            sender.sendMessage("§aBackup completed: " + result.getBackupInfo().getFileName());
                        } else {
                            sender.sendMessage("§cBackup failed: " + result.getMessage());
                        }
                    });
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1)
            return List.of();
        return List.of("about", "status", "start", "stop", "backup");
    }
}
