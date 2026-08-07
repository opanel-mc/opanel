package net.opanel.forge_helper;

import com.cozooo.dlc_fileops_helper.api.FileOpsHelperApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public abstract class BaseForgeServer implements OPanelServer {
    protected final MinecraftServer server;

    public BaseForgeServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.FORGE;
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
    public String getCurrentSaveName() {
        return server.getWorldPath(LevelResource.LEVEL_DATA_FILE).getParent().getFileName().toString();
    }

    @Override
    public void saveAll() {
        server.saveEverything(true, true, true);
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
        manager.performPrefixedCommand(source, command);
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
    public Path getPluginsPath() {
        return Paths.get("").resolve("mods").toAbsolutePath();
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

    @Override
    public void updatePlugin(String fileName, Path newPluginFile) throws IOException, ActLaterException {
        Path modsPath = getPluginsPath();
        Path targetPath = modsPath.resolve(fileName);
        if(!Files.exists(targetPath)) {
            targetPath = modsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        }

        if(!Files.exists(targetPath)) {
            throw new NoSuchFileException("Mod file not found: " + fileName);
        }

        try {
            Files.move(newPluginFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // The mod is loaded and the file cannot be overwritten, defer it to the server restart
            Path deferredPath = modsPath.resolve(fileName + ".update");
            try {
                Files.move(newPluginFile, deferredPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                throw new IOException("Cannot update a loaded mod.");
            }
            FileOpsHelperApi.scheduleMove(deferredPath.toString(), targetPath.toString(), true);
            throw new ActLaterException();
        }
    }
}

