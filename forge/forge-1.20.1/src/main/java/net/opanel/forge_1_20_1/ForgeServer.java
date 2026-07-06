package net.opanel.forge_1_20_1;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.opanel.common.*;
import net.opanel.exception.MissingPlayerCacheException;
import net.opanel.forge_helper.BaseForgeServer;
import net.opanel.forge_helper.utils.ForgeUtils;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ForgeServer extends BaseForgeServer implements OPanelServer {
    private final ForgeChunkAccessor chunkAccessor;

    public ForgeServer(MinecraftServer server) {
        super(server);

        chunkAccessor = new ForgeChunkAccessor(server);
    }

    @Override
    public byte[] getFavicon() {
        byte[] serverIconPNG = super.getFavicon();
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
        super.setFavicon(iconBytes);
        // reload server favicon
        ServerStatus status = server.getStatus();
        ServerStatus.Favicon favicon = new ServerStatus.Favicon(iconBytes);
        ServerStatus newStatus = new ServerStatus(
                status.description(),
                status.players(),
                status.version(),
                Optional.of(favicon),
                status.enforcesSecureChat(),
                status.forgeData()
        );
        try {
            Field statusIconField = MinecraftServer.class.getDeclaredField("f_271173_"); // f_271173_ -> statusIcon
            statusIconField.setAccessible(true);
            statusIconField.set(server, favicon);

            Field statusField = MinecraftServer.class.getDeclaredField("f_129757_"); // f_129757_ -> status
            statusField.setAccessible(true);
            statusField.set(server, newStatus);
        } catch (Exception e) {
            Main.LOGGER.warn("Cannot reload server favicon.");
        }
    }

    @Override
    public List<OPanelSave> getSaves() {
        List<OPanelSave> list = new ArrayList<>();
        try(Stream<Path> stream = Files.list(Paths.get(""))) {
            stream.filter(path -> (
                            Files.exists(path.resolve("level.dat"))
                                    && !Files.isDirectory(path.resolve("level.dat"))
                    ))
                    .map(Path::toAbsolutePath)
                    .forEach(path -> {
                        ForgeSave save = new ForgeSave(server, path);
                        list.add(save);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public OPanelSave getSave(String saveName) {
        final Path savePath = Paths.get("").resolve(saveName);
        if(!Files.exists(savePath) || !Files.exists(savePath.resolve("level.dat"))) {
            return null;
        }
        return new ForgeSave(server, savePath.toAbsolutePath());
    }

    @Override
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for(ServerPlayer serverPlayer : players) {
            ForgePlayer player = new ForgePlayer(serverPlayer);
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

                            ForgeOfflinePlayer player = new ForgeOfflinePlayer(server, UUID.fromString(uuid));
                            list.add(player);
                        } catch (MissingPlayerCacheException e) {
                            //
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
        return new ForgeWhitelist(server.getPlayerList().getWhiteList());
    }

    @Override
    public HashMap<String, Object> getGamerules(OPanelDimension dimension) {
        final CompoundTag gamerulesNbt = ForgeUtils.getLevelByDimension(server, dimension).getGameRules().createTag();
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
    public void setGamerules(OPanelDimension dimension, HashMap<String, Object> gamerules) {
        HashMap<String, Object> currentGamerules = getGamerules(dimension);
        final GameRules gameRulesObj = ForgeUtils.getLevelByDimension(server, dimension).getGameRules();
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
    public long getIngameTime() {
        return server.overworld().getDayTime();
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
            if(modId.equals("minecraft") || modId.equals("forge")) {
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
    public OPanelChunkAccessor getChunkAccessor() {
        return chunkAccessor;
    }
}
