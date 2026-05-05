package net.opanel.fabric_helper_unmapped;

import com.cozooo.dlc_fileops_helper.api.FileOpsHelperApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import net.opanel.exception.ActLaterException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public abstract class BaseFabricServer implements OPanelServer {
    protected final MinecraftServer server;

    public BaseFabricServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.FABRIC;
    }

    @Override
    public String getMotd() {
        return server.getMotd();
    }

    @Override
    public void setMotd(String motd) throws IOException {
        // Call setMotd() first
        server.setMotd(motd);
        // Directly modify motd in server.properties
        String formatted = motd.replaceAll("\n", Matcher.quoteReplacement("\\n"));
        OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("motd=.+", Matcher.quoteReplacement("motd="+ formatted)));
    }

    @Override
    public String getVersion() {
        return server.getServerVersion();
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Override
    public void saveAll() {
        runTask(() -> server.saveEverything(true, true, true));
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
        final Path playerDataFolder = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat"));
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat_old"));
    }

    @Override
    public void sendServerCommand(String command) {
        Commands manager = server.getCommands();
        CommandSourceStack source = server.createCommandSourceStack();
        runTask(() -> manager.performPrefixedCommand(source, command));
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        for(CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChildren()) {
            commands.add(node.getName());
        }
        return commands;
    }

    @Override
    public List<String> getCommandTabList(int argIndex, String command) {
        if(argIndex == 1) return getCommands();

        List<String> tabList = new ArrayList<>();
        String[] args = command.split(" ");
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        CommandNode<CommandSourceStack> currentNode = dispatcher.getRoot();
        for(int i = 0; i <= args.length; i++) {
            if(currentNode == null) break;
            if(i + 1 == argIndex) {
                for(CommandNode<CommandSourceStack> subNode : currentNode.getChildren()) {
                    tabList.add(subNode.getName());
                }
                break;
            }
            if(i == args.length) break;
            currentNode = currentNode.getChild(args[i]);
        }
        return tabList;
    }

    @Override
    public void reload() {
        // directly execute /reload
        sendServerCommand("reload");
    }

    @Override
    public void stop() {
        server.halt(false);
    }

    @Override
    public long getIngameTime() {
        return server.overworld().getOverworldClockTime();
    }

    @Override
    public Path getPluginsPath() {
        return Paths.get("").resolve("mods").toAbsolutePath();
    }

    @Override
    public List<OPanelPlugin> getPlugins() {
        List<OPanelPlugin> mods = new ArrayList<>();
        Path modsPath = getPluginsPath();
        List<String> loadedPluginFileNames = new ArrayList<>();
        
        // Get loaded mods from FabricLoader
        for(ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            ModMetadata metadata = modContainer.getMetadata();
            
            // Skip built-in mods (fabric-api internals, minecraft, java, etc.)
            String modId = metadata.getId();
            if(modId.equals("minecraft") || modId.equals("java") || modId.equals("fabricloader")) {
                continue;
            }

            ModOrigin origin = modContainer.getOrigin();
            if(origin.getKind() == ModOrigin.Kind.NESTED) continue;

            try {
                Path filePath = origin.getPaths().get(0);
                String fileName = filePath.getFileName().toString();
                long fileSize = Files.size(filePath);

                List<String> authorList = new ArrayList<>();
                for(Person author : metadata.getAuthors()) {
                    authorList.add(author.getName());
                }

                String website = null;
                Optional<String> websiteOptional = metadata.getContact().get("homepage");
                if(websiteOptional.isPresent()) {
                    website = websiteOptional.get();
                }

                byte[] icon = null;
                Optional<String> iconPathStrOptional = metadata.getIconPath(64);
                if(iconPathStrOptional.isPresent()) {
                    Optional<Path> iconPathOptional = modContainer.findPath(iconPathStrOptional.get());
                    if(iconPathOptional.isPresent()) {
                        icon = Files.readAllBytes(iconPathOptional.get());
                    }
                }

                mods.add(new OPanelPlugin(
                        fileName,
                        metadata.getName(),
                        metadata.getVersion().getFriendlyString(),
                        metadata.getDescription(),
                        authorList,
                        website,
                        icon,
                        fileSize,
                        true, // All loaded mods are enabled
                        true  // All loaded mods are loaded
                ));

                loadedPluginFileNames.add(fileName);
            } catch (IOException e) {
                //
            }
        }
        
        // Scan for disabled mods (.jar.disabled files)
        try(Stream<Path> stream = Files.list(modsPath)) {
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
                        mods.add(new OPanelPlugin(
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
        
        return mods;
    }

    @Override
    public void togglePlugin(String fileName, boolean enabled) throws IOException, ActLaterException {
        Path modsPath = getPluginsPath();
        Path originalPath = modsPath.resolve(fileName);
        if(!Files.exists(originalPath)) {
            throw new NoSuchFileException("Mod file not found: " + fileName);
        }

        final boolean isActuallyDisabled = fileName.endsWith(OPanelPlugin.DISABLED_SUFFIX);

        if(isActuallyDisabled && enabled) {
            // Rename from .jar.disabled to .jar
            Path newPath = modsPath.resolve(fileName.replaceAll("\\"+ OPanelPlugin.DISABLED_SUFFIX +"$", ""));
            try {
                Files.move(originalPath, newPath);
            } catch (Exception e) {
                FileOpsHelperApi.scheduleMove(originalPath.toString(), newPath.toString(), true);
                throw new ActLaterException();
            }
        } else if(!isActuallyDisabled && !enabled) {
            // Rename from .jar to .jar.disabled
            Path newPath = modsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
            try {
                Files.move(originalPath, newPath);
            } catch (Exception e) {
                FileOpsHelperApi.scheduleMove(originalPath.toString(), newPath.toString(), true);
                throw new ActLaterException();
            }
        } else if(!isActuallyDisabled) {
            // Cancel the pending operation of renaming from .jar to .jar.disabled
            Path targetPath = modsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
            FileOpsHelperApi.cancelPendingOperationsByTarget(List.of(targetPath.toString()));
            throw new ActLaterException();
        }
    }

    @Override
    public void deletePlugin(String fileName) throws IOException, ActLaterException {
        Path modsPath = getPluginsPath();
        Path filePath = modsPath.resolve(fileName);

        if(!Files.exists(filePath)) {
            // Try with .disabled suffix
            filePath = modsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        }

        if(!Files.exists(filePath)) {
            throw new NoSuchFileException("Mod file not found: " + fileName);
        }

        try {
            Files.delete(filePath);
        } catch (Exception e) {
            FileOpsHelperApi.scheduleDelete(List.of(filePath.toString()));
            throw new ActLaterException();
        }
    }

    protected void runTask(Runnable task) {
        Object lock = new Object();
        synchronized(lock) {
            server.execute(() -> {
                task.run();
                synchronized(lock) {
                    lock.notify();
                }
            });
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
