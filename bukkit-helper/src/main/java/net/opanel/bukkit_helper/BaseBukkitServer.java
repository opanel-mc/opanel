package net.opanel.bukkit_helper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import org.bukkit.*;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseBukkitServer implements OPanelServer {
    protected final JavaPlugin plugin;
    protected final TaskRunner runner;
    protected final Server server;

    public BaseBukkitServer(JavaPlugin plugin, Server server) {
        this.plugin = plugin;
        runner = (TaskRunner) plugin;
        this.server = server;
    }

    @Override
    public String getMotd() {
        return server.getMotd();
    }

    @Override
    public String getVersion() {
        // getBukkitVersion() -> "<MinecraftVersion>-R0.x-SNAPSHOT"
        return server.getBukkitVersion().split("-")[0];
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Override
    public void saveAll() {
        runner.runTask(() -> {
            for(World world : server.getWorlds()) {
                world.save();
            }
            server.savePlayers();
        });
    }

    @Override
    public int getMaxPlayerCount() {
        return server.getMaxPlayers();
    }

    @Override
    public OPanelPlayer getPlayer(String uuid) {
        for(OPanelPlayer player : getPlayers()) {
            if(player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public void removePlayerData(String uuid) throws IOException {
        final Path playerDataFolder = server.getWorlds().get(0).getWorldFolder().toPath().resolve("playerdata");
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat"));
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat_old"));
    }

    @Override
    public List<String> getBannedIps() {
        return new ArrayList<>(server.getIPBans());
    }

    @Override
    public boolean isWhitelistEnabled() {
        return server.hasWhitelist();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        runner.runTask(() -> server.setWhitelist(enabled));
    }

    @Override
    public void sendServerCommand(String command) {
        runner.runTask(() -> Bukkit.dispatchCommand(server.getConsoleSender(), command));
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        for(HelpTopic topic : server.getHelpMap().getHelpTopics()) {
            commands.add(topic.getName().toLowerCase().replaceFirst("/", ""));
        }
        return commands;
    }

    @Override
    public void reload() {
        runner.runTask(server::reload);
    }

    @Override
    public void stop() {
        server.shutdown();
    }

    @Override
    public long getIngameTime() {
        return server.getWorlds().get(0).getTime();
    }
}
