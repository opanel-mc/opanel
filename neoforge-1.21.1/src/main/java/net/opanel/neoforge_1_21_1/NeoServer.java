package net.opanel.neoforge_1_21_1;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.opanel.common.ServerType;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class NeoServer implements OPanelServer {
    private final MinecraftServer server;
    private final DedicatedServer dedicatedServer;

    public NeoServer(MinecraftServer server) {
        this.server = server;
        dedicatedServer = (DedicatedServer) server;
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
    public void setFavicon(byte[] iconBytes) throws IOException {
        OPanelServer.super.setFavicon(iconBytes);
        // reload server favicon
        ServerStatus status = server.getStatus();
        ServerStatus.Favicon favicon = new ServerStatus.Favicon(iconBytes);
        ServerStatus newStatus = new ServerStatus(
                status.description(),
                status.players(),
                status.version(),
                Optional.of(favicon),
                status.enforcesSecureChat(),
                status.isModded()
        );
        try {
            Field statusIconField = MinecraftServer.class.getDeclaredField("statusIcon");
            statusIconField.setAccessible(true);
            statusIconField.set(server, favicon);

            Field statusField = MinecraftServer.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(server, newStatus);
        } catch (Exception e) {
            Main.LOGGER.warn("Cannot reload server favicon.");
        }
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
    public List<OPanelSave> getSaves() {
        List<OPanelSave> list = new ArrayList<>();
        Path rootPath = getSavesRootPath();
        try(Stream<Path> stream = Files.list(rootPath)) {
            stream.filter(path -> (
                            Files.exists(path.resolve("level.dat"))
                                    && !Files.isDirectory(path.resolve("level.dat"))
                    ))
                    .map(Path::toAbsolutePath)
                    .forEach(path -> {
                        NeoSave save = new NeoSave(server, path);
                        list.add(save);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public OPanelSave getSave(String saveName) {
        Path rootPath = getSavesRootPath();
        final Path savePath = rootPath.resolve(saveName);
        if(!Files.exists(savePath) || !Files.exists(savePath.resolve("level.dat"))) {
            return null;
        }
        return new NeoSave(server, savePath.toAbsolutePath());
    }

    private Path getSavesRootPath() {
        Path levelDataPath = server.getWorldPath(LevelResource.LEVEL_DATA_FILE);
        Path worldRoot = levelDataPath.getParent();
        if (worldRoot == null) {
            return Paths.get("").toAbsolutePath();
        }
        Path parent = worldRoot.getParent();
        return (parent != null ? parent : worldRoot).toAbsolutePath();
    }

    @Override
    public void saveAll() {
        server.saveEverything(true, true, true);
    }

    @Override
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for(ServerPlayer serverPlayer : players) {
            NeoPlayer player = new NeoPlayer(serverPlayer);
            list.add(player);
        }
        return list;
    }

    @Override
    public List<OPanelPlayer> getPlayers() {
        final Path playerDataPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        // load online players
        List<OPanelPlayer> list = new ArrayList<>(getOnlinePlayers());

        // load offline players
        try(Stream<Path> stream = Files.list(playerDataPath)) {
            stream.filter(item -> !Files.isDirectory(item) && item.toString().endsWith(".dat"))
                    .forEach(item -> {
                        try {
                            final String uuid = item.getFileName().toString().replace(".dat", "");
                            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(UUID.fromString(uuid));
                            if(serverPlayer != null && !serverPlayer.hasDisconnected()) return;

                            NeoOfflinePlayer player = new NeoOfflinePlayer(server, UUID.fromString(uuid));
                            list.add(player);
                        } catch (Exception e) {
                            Main.LOGGER.warn("Cannot read the player data from "+ item.getFileName() +": "+ e.getMessage());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            return list;
        }
        return list;
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
    public OPanelWhitelist getWhitelist() {
        return new NeoWhitelist(server.getPlayerList().getWhiteList());
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
    public HashMap<String, Object> getGamerules() {
        final CompoundTag gamerulesNbt = server.getGameRules().createTag();
        HashMap<String, Object> gamerules = new HashMap<>();
        for(String key : gamerulesNbt.getAllKeys()) {
            final String valueStr = gamerulesNbt.getString(key);
            if(valueStr.equals("true") || valueStr.equals("false")) {
                gamerules.put(key, Boolean.valueOf(valueStr));
            } else if(Utils.isNumeric(valueStr)) {
                gamerules.put(key, Integer.valueOf(valueStr));
            } else {
                gamerules.put(key, valueStr);
            }
        }
        return gamerules;
    }

    @Override
    public void setGamerules(HashMap<String, Object> gamerules) {
        HashMap<String, Object> currentGamerules = getGamerules();
        final GameRules gameRulesObj = server.getGameRules();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                GameRules.GameRuleTypeVisitor.super.visit(key, type);

                final String ruleName = key.getId();
                final Object value = gamerules.get(ruleName);
                if(value == null) return;
                final Object currentValue = currentGamerules.get(ruleName);
                if(value.equals(currentValue)) return;

                T rule = type.createRule();
                if(rule instanceof GameRules.BooleanValue) { // boolean
                    ((GameRules.BooleanValue) rule).set((boolean) value, server);
                    gameRulesObj.getRule(key).setFrom(rule, server);
                } else if(rule instanceof GameRules.IntegerValue) { // integer
                    int n = ((Number) value).intValue();
                    ((GameRules.IntegerValue) rule).set(n, server);
                    gameRulesObj.getRule(key).setFrom(rule, server);
                } else { // string
                    sendServerCommand("gamerule "+ ruleName +" "+ value);
                }
            }
        });
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
    public void togglePlugin(String fileName, boolean enabled) throws IOException {
        Path pluginsPath = getPluginsPath();
        Path originalPath = pluginsPath.resolve(fileName);
        if(!Files.exists(originalPath)) {
            throw new NoSuchFileException("Mod file not found: " + fileName);
        }

        final boolean isActuallyDisabled = fileName.endsWith(OPanelPlugin.DISABLED_SUFFIX);

        if(isActuallyDisabled && enabled) {
            // Rename from .jar.disabled to .jar
            Path newPath = pluginsPath.resolve(fileName.replaceAll("\\"+ OPanelPlugin.DISABLED_SUFFIX +"$", ""));
            Files.move(originalPath, newPath);
        } else if(!isActuallyDisabled && !enabled) {
            for(IModInfo modInfo : ModList.get().getMods()) {
                if(fileName.equals(modInfo.getOwningFile().getFile().getFileName())) {
                    throw new IllegalStateException("Cannot disable a loaded mod.");
                }
            }

            // Rename from .jar to .jar.disabled
            Path newPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
            Files.move(originalPath, newPath);
        }
    }

    @Override
    public void deletePlugin(String fileName) throws IOException {
        Path modsPath = getPluginsPath();
        Path filePath = modsPath.resolve(fileName);

        if(!Files.exists(filePath)) {
            // Try with .disabled suffix
            filePath = modsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        }

        if(!Files.exists(filePath)) {
            throw new NoSuchFileException("Mod file not found: " + fileName);
        }

        Files.delete(filePath);
    }
}
