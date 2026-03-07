package net.opanel.bukkit_helper;

import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import net.opanel.utils.Utils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
    public ServerType getServerType() {
        if(Utils.hasClass("org.leavesmc.leaves.LeavesConfig")) {
            return ServerType.LEAVES;
        }
        if(Utils.hasClass("com.destroystokyo.paper.PaperConfig")) {
            return ServerType.PAPER;
        }
        if(Utils.hasClass("org.bukkit.entity.Player$Spigot")) {
            return ServerType.SPIGOT;
        }
        return ServerType.BUKKIT;
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
            String fileName = URLDecoder.decode(p.getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), StandardCharsets.UTF_8);
            // Extract just the filename
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            
            try {
                long fileSize = Files.size(pluginsPath.resolve(fileName));

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
            } catch (IOException e) {
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
        if (!Files.exists(originalPath)) {
            throw new NoSuchFileException("Plugin file not found: " + fileName);
        }

        runner.runTask(() -> {
            try {
                final boolean isActuallyDisabled = fileName.endsWith(OPanelPlugin.DISABLED_SUFFIX);

                if (isActuallyDisabled && enabled) {
                    String newFileName = fileName.substring(0, fileName.length() - OPanelPlugin.DISABLED_SUFFIX.length());
                    Path newPath = pluginsPath.resolve(newFileName);

                    Files.move(originalPath, newPath);

                    performLoad(newPath);
                }

                else if (!isActuallyDisabled && !enabled) {
                    Plugin target = null;
                    for (Plugin p : server.getPluginManager().getPlugins()) {
                        String itemName = URLDecoder.decode(p.getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), StandardCharsets.UTF_8);
                        itemName = itemName.substring(itemName.lastIndexOf('/') + 1);
                        if (fileName.equals(itemName)) {
                            target = p;
                            break;
                        }
                    }

                    if (target != null) {
                        performFullUnload(target);
                    }

                    Path newPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
                    Files.move(originalPath, newPath);

                    syncCommands();
                }
            } catch (IOException e) {
                throw new RuntimeException("Toggle plugin failed due to I/O: " + fileName + ". " + e.getMessage());
            }
        });
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
        
        Files.delete(filePath);
    }

    private void performFullUnload(Plugin target) {
        String name = target.getName();
        PluginManager pm = server.getPluginManager();

        // disable the plugin
        if (target.isEnabled()) {
            pm.disablePlugin(target);
        }

        try {
            // remove the plugin
            Field pluginsField = pm.getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            List<Plugin> plugins = (List<Plugin>) pluginsField.get(pm);

            Field lookupNamesField = pm.getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(pm);

            synchronized (pm) {
                plugins.remove(target);
                lookupNames.remove(name.toLowerCase());
            }

            // clean the map
            Field commandMapField = pm.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(pm);

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            synchronized (commandMap) {
                Iterator<Map.Entry<String, Command>> it = knownCommands.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Command> entry = it.next();
                    if (entry.getValue() instanceof PluginCommand) {
                        PluginCommand pc = (PluginCommand) entry.getValue();
                        if (pc.getPlugin().equals(target)) {
                            pc.unregister(commandMap);
                            it.remove();
                        }
                    }
                }
            }

            // release the plugin file
            ClassLoader cl = target.getClass().getClassLoader();
            if (cl instanceof URLClassLoader) {
                ((URLClassLoader) cl).close();
            }

            System.gc();

        } catch (Exception e) {
            throw new RuntimeException("An error occurred when disabling the plugin " + name + ": " + e.getMessage());
        }
    }

    private void performLoad(Path pluginPath) {
        try {
            Plugin target = server.getPluginManager().loadPlugin(pluginPath.toFile());

            if (target != null) {
                server.getPluginManager().enablePlugin(target);

                syncCommands();
            }
        } catch (InvalidPluginException | InvalidDescriptionException | UnknownDependencyException e) {
            server.getLogger().severe("Failed to load the plugin " + pluginPath.getFileName() + ": " + e.getMessage());
        }
    }

    private void syncCommands() {
        // Minecraft 1.13+
        try {
            java.lang.reflect.Method syncMethod = server.getClass().getDeclaredMethod("syncCommands");
            syncMethod.setAccessible(true);
            syncMethod.invoke(server);
        } catch (Exception ignored) {
            //
        }
    }
}

