package net.opanel.neoforge_helper;

import com.cozooo.dlc_fileops_helper.api.FileOpsHelperApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.opanel.common.*;
import net.opanel.exception.ActLaterException;
import net.opanel.neoforge_helper.utils.NeoUtils;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public abstract class BaseNeoServer implements OPanelServer {
    protected final MinecraftServer server;

    public BaseNeoServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.NEOFORGE;
    }

    @Override
    public byte[] getFavicon() {
        byte[] serverIconPNG = OPanelServer.super.getFavicon();
        if(serverIconPNG != null) return serverIconPNG;

        ServerStatus status = server.getStatus();
        if(status == null) return null;

        Optional<ServerStatus.Favicon> faviconOptional = status.favicon();
        if(faviconOptional.isEmpty()) return null;

        ServerStatus.Favicon favicon = faviconOptional.get();
        return favicon.iconBytes();
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
    public List<String> getBannedIps() {
        Collection<IpBanListEntry> entries = server.getPlayerList().getIpBans().getEntries();
        List<String> list = new ArrayList<>();
        entries.forEach(entry -> list.add(entry.getDisplayName().getString()));
        return list;
    }

    @Override
    public void banIp(String ip) {
        if(getBannedIps().contains(ip)) return;
        server.getPlayerList().getIpBans().add(new IpBanListEntry(ip));
    }

    @Override
    public void pardonIp(String ip) {
        if(!getBannedIps().contains(ip)) return;
        server.getPlayerList().getIpBans().remove(ip);
    }

    @Override
    public boolean isWhitelistEnabled() {
        return server.getPlayerList().isUsingWhitelist();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        server.getPlayerList().setUsingWhiteList(enabled);
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
    public long getIngameTime() {
        return server.overworld().getDayTime();
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

        // Get loaded mods from Forge ModList
        for(IModInfo modInfo : ModList.get().getMods()) {
            String modId = modInfo.getModId();

            // Skip built-in mods
            if(modId.equals("minecraft") || modId.equals("neoforge")) {
                continue;
            }

            final IModFile modFile = modInfo.getOwningFile().getFile();

            try {
                String fileName = modFile.getFileName();
                long fileSize = Files.size(modFile.getFilePath());

                List<String> authors = modInfo.getConfig().getConfigElement("authors")
                        .map(obj -> List.of(obj.toString().split(",")))
                        .map(list -> list.stream().map(String::trim).toList())
                        .orElse(List.of());

                String website = null;
                Optional<URL> modUrlOptional = modInfo.getModURL();
                if(modUrlOptional.isPresent()) {
                    website = modUrlOptional.get().toString();
                }

                byte[] icon = null;
                Optional<String> logoFileOptional = modInfo.getLogoFile();
                if(logoFileOptional.isPresent()) {
                    Path iconPath = modFile.findResource(logoFileOptional.get());
                    if(Files.exists(iconPath)) {
                        icon = Files.readAllBytes(iconPath);
                    }
                }

                mods.add(new OPanelPlugin(
                        fileName,
                        modInfo.getDisplayName(),
                        modInfo.getVersion().toString(),
                        modInfo.getDescription(),
                        authors,
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
