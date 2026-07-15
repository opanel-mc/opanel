package net.opanel.paper_helper;

import net.opanel.paper_helper.utils.PaperUtils;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import org.bukkit.*;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class BasePaperServer implements OPanelServer {
    protected final JavaPlugin plugin;
    protected final TaskRunner runner;
    protected final Server server;

    public BasePaperServer(JavaPlugin plugin, Server server) {
        this.plugin = plugin;
        runner = (TaskRunner) plugin;
        this.server = server;
    }

    @Override
    public ServerType getServerType() {
        if(PaperUtils.isLeaves()) {
            return ServerType.LEAVES;
        }
        return ServerType.PAPER;
    }

    @Override
    public String getMotd() {
        return server.getMotd();
    }

    @Override
    public String getVersion() {
        String version = server.getBukkitVersion();
        // "<MinecraftVersion>-R0.x-SNAPSHOT" -> MinecraftVersion
        version = version.split("-")[0];
        // "<MinecraftVersion>.build.<PaperBuildVersion>" -> MinecraftVersion
        version = version.split(".build.")[0];

        return version;
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Override
    public String getCurrentSaveName() {
        return server.getWorlds().get(0).getName();
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

    @Override
    public Path getPluginsPath() {
        return plugin.getDataFolder().getParentFile().toPath();
    }

    @Override
    public List<OPanelPlugin> getPlugins() {
        List<OPanelPlugin> plugins = new ArrayList<>();
        Path pluginsPath = getPluginsPath();
        List<String> loadedPluginFileNames = new ArrayList<>();
        
        // Get loaded plugins from Bukkit
        for(Plugin p : server.getPluginManager().getPlugins()) {
            PluginDescriptionFile desc = p.getDescription();
            
            try {
                File pluginFile = new File(p.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                String fileName = pluginFile.getName();
                long fileSize = Files.size(pluginFile.toPath());

                plugins.add(new OPanelPlugin(
                        fileName,
                        desc.getName(),
                        desc.getVersion(),
                        desc.getDescription(),
                        desc.getAuthors(),
                        desc.getWebsite(),
                        null,
                        fileSize,
                        p.isEnabled(),
                        true
                ));

                loadedPluginFileNames.add(fileName);
            } catch (Exception e) {
                //
            }
        }
        
        // Scan for disabled or unloaded plugins
        try(Stream<Path> stream = Files.list(pluginsPath)) {
            stream.filter(path -> (
                    (path.toString().endsWith(".jar"+ OPanelPlugin.DISABLED_SUFFIX) || path.toString().endsWith(".jar"))
                    && !loadedPluginFileNames.contains(path.getFileName().toString())
                ))
                .forEach(path -> {
                    try {
                        String fileName = path.getFileName().toString();
                        String name = fileName.replaceAll("\\.jar(\\"+ OPanelPlugin.DISABLED_SUFFIX +")?$", "");
                        long fileSize = Files.size(path);
                        boolean enabled = path.toString().endsWith(".jar");
                        plugins.add(new OPanelPlugin(
                            fileName,
                            name,
                            null,
                            null,
                            null,
                            null,
                            null,
                            fileSize,
                            enabled,
                            false
                        ));
                    } catch (IOException e) {
                        //
                    }
                });
        } catch (IOException e) {
            //
        }
        
        return plugins;
    }

    @Override
    public void togglePlugin(String fileName, boolean enabled) throws IOException {
        Path pluginsPath = getPluginsPath();
        Path originalPath = pluginsPath.resolve(fileName);
        if(!Files.exists(originalPath)) {
            throw new NoSuchFileException("Plugin file not found: " + fileName);
        }

        final boolean isActuallyDisabled = fileName.endsWith(OPanelPlugin.DISABLED_SUFFIX);

        if(isActuallyDisabled && enabled) {
            // Rename from .jar.disabled to .jar
            Path newPath = pluginsPath.resolve(fileName.replaceAll("\\"+ OPanelPlugin.DISABLED_SUFFIX +"$", ""));
            Files.move(originalPath, newPath);
        } else if(!isActuallyDisabled && !enabled) {
            // Rename from .jar to .jar.disabled
            Path newPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
            try {
                Files.move(originalPath, newPath);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot disable a loaded plugin.");
            }
        }
    }

    @Override
    public void deletePlugin(String fileName) throws IOException {
        Path pluginsPath = getPluginsPath();
        Path filePath = pluginsPath.resolve(fileName);
        
        if(!Files.exists(filePath)) {
            // Try with .disabled suffix
            filePath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        }
        
        if(!Files.exists(filePath)) {
            throw new NoSuchFileException("Plugin file not found: " + fileName);
        }
        
        try {
            Files.delete(filePath);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot delete a loaded plugin.");
        }
    }
}

