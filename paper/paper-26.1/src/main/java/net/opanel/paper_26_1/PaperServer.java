package net.opanel.paper_26_1;

import com.cozooo.dlc_fileops_helper.api.FileOpsHelperApi;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.kyori.adventure.text.Component;
import net.opanel.annotation.Rewrite;
import net.opanel.paper_helper.BasePaperServer;
import net.opanel.paper_helper.utils.PaperUtils;
import net.opanel.common.*;
import net.opanel.exception.MissingPlayerCacheException;
import net.opanel.common.features.PaperConfigFeature;
import net.opanel.common.features.CodeOfConductFeature;
import net.opanel.exception.ActLaterException;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class PaperServer extends BasePaperServer implements OPanelServer, CodeOfConductFeature, PaperConfigFeature {
    private final PaperChunkAccessor chunkAccessor;

    public PaperServer(Main plugin, Server server) {
        super(plugin, server);

        this.chunkAccessor = new PaperChunkAccessor(plugin);
    }

    @Override
    public void setFavicon(byte[] iconBytes) throws IOException {
        super.setFavicon(iconBytes);
        // reload server favicon
        try {
            Method loadIconMethod = server.getClass().getDeclaredMethod("loadIcon");
            loadIconMethod.setAccessible(true);
            loadIconMethod.invoke(server);
        } catch (Exception e) {
            ((Main) plugin).LOGGER.warning("Cannot reload server favicon.");
        }
    }

    @Override
    public void setMotd(String motd) throws IOException {
        // Call setMotd() first
        server.motd(Component.text(motd));
        // Directly modify motd in server.properties
        String formatted = motd.replaceAll("\n", Matcher.quoteReplacement("\\n"));
        OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("motd=.+", Matcher.quoteReplacement("motd="+ formatted)));
    }

    @Override
    public List<OPanelSave> getSaves() {
        List<OPanelSave> list = new ArrayList<>();
        try(Stream<Path> stream = Files.list(Paths.get(""))) {
            stream.filter(path -> (
                            !path.toString().endsWith("_nether")
                            && !path.toString().endsWith("_the_end")
                            && Files.exists(path.resolve("level.dat"))
                            && !Files.isDirectory(path.resolve("level.dat"))
                    ))
                    .map(Path::toAbsolutePath)
                    .forEach(path -> {
                        PaperSave save = new PaperSave((Main) plugin, server, path);
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
        if(
                !Files.exists(savePath)
                || savePath.toString().endsWith("_nether")
                || savePath.toString().endsWith("_the_end")
                || !Files.exists(savePath.resolve("level.dat"))
        ) {
            return null;
        }
        return new PaperSave((Main) plugin, server, savePath.toAbsolutePath());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        Collection<Player> players = (Collection<Player>) server.getOnlinePlayers();
        for(Player serverPlayer : players) {
            PaperPlayer player = new PaperPlayer((Main) plugin, serverPlayer);
            list.add(player);
        }
        return list;
    }

    @Override
    public List<OPanelPlayer> getPlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        OfflinePlayer[] players = server.getOfflinePlayers();
        for(OfflinePlayer offlinePlayer : players) {
            if(offlinePlayer.isOnline()) {
                Player serverPlayer = offlinePlayer.getPlayer();
                if(serverPlayer == null) continue;
                list.add(new PaperPlayer((Main) plugin, serverPlayer));
            } else {
                try {
                    list.add(new PaperOfflinePlayer((Main) plugin, server, offlinePlayer));
                } catch (MissingPlayerCacheException e) {
                    //
                }
            }
        }
        return list;
    }

    @Rewrite
    @Override
    public void removePlayerData(String uuid) throws IOException {
        final Path playerDataFolder = server.getWorlds().getFirst().getWorldFolder().toPath()
                .resolve("../../../players/data");
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat"));
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat_old"));
    }

    @Override
    public void banIp(String ip) throws UnknownHostException {
        if(server.getIPBans().contains(ip)) return;
        server.banIP(InetAddress.getByName(ip));
    }

    @Override
    public void pardonIp(String ip) throws UnknownHostException {
        if(!server.getIPBans().contains(ip)) return;
        server.unbanIP(InetAddress.getByName(ip));
    }

    @Override
    public OPanelWhitelist getWhitelist() {
        return new PaperWhitelist((Main) plugin, server, server.getWhitelistedPlayers());
    }

    @Override
    public void sendServerCommand(String command) {
        runner.runTask(() -> {
            try {
                PaperUtils.performCommand(command, false);
            } catch (ReflectiveOperationException e) {
                //
            }
        });
    }

    @Override
    public List<String> getCommandTabList(int argIndex, String command) {
        if(argIndex == 1) return getCommands();

        List<String> tabList = new ArrayList<>();
        String[] args = command.split(" ");

        try {
            CommandDispatcher<?> dispatcher = PaperUtils.getCommandDispatcher(false);
            CommandNode<?> currentNode = dispatcher.getRoot();
            for(int i = 0; i <= args.length; i++) {
                if(currentNode == null) break;
                if(i + 1 == argIndex) {
                    for(CommandNode<?> subNode : currentNode.getChildren()) {
                        tabList.add(subNode.getName());
                    }
                    break;
                }
                if(i == args.length) break;
                currentNode = currentNode.getChild(args[i]);
            }
        } catch (Exception e) {
            //
        }
        return tabList;
    }

    @Override
    public HashMap<String, Object> getGamerules(OPanelDimension dimension) {
        final World world = PaperUtils.getWorldByDimension(dimension);
        HashMap<String, Object> gamerules = new HashMap<>();
        try {
            // Paper changed GameRule from interface to class, causing
            // IncompatibleClassChangeError on any direct method call.
            // All GameRule interactions must go through reflection.
            // The same as setGamerules()
            Class<?> gameRuleClass = GameRule.class;
            Method getNamespacedKey = gameRuleClass.getMethod("getKey");
            Method getKey = NamespacedKey.class.getMethod("getKey");
            Method getGameRuleValue = World.class.getMethod("getGameRuleValue", gameRuleClass);

            for(Object rule : Lists.newArrayList(Registry.GAME_RULE)) {
                try {
                    Object key = getNamespacedKey.invoke(rule);
                    if(key == null) continue;
                    gamerules.put((String) getKey.invoke(key), getGameRuleValue.invoke(world, (GameRule<?>) rule));
                } catch (Exception e) {
                    //
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gamerules;
    }

    @Override
    public void setGamerules(OPanelDimension dimension, HashMap<String, Object> gamerules) {
        HashMap<String, Object> currentGamerules = getGamerules(dimension);
        runner.runTask(() -> {
            final World world = PaperUtils.getWorldByDimension(dimension);
            try {
                Class<?> gameRuleClass = GameRule.class;
                Method getByName = gameRuleClass.getMethod("getByName", String.class);
                Method setGameRule = World.class.getMethod("setGameRule", gameRuleClass, Object.class);

                gamerules.forEach((key, value) -> {
                    if(value == null) return;
                    final Object currentValue = currentGamerules.get(key);
                    if(value.equals(currentValue)) return;

                    try {
                        Object rule = getByName.invoke(null, key);
                        if(rule == null) return;

                        if(value instanceof Boolean) {
                            setGameRule.invoke(world, (GameRule<?>) rule, value);
                        } else if(value instanceof Number) {
                            int n = ((Number) value).intValue();
                            setGameRule.invoke(world, (GameRule<?>) rule, n);
                        } else {
                            sendServerCommand("gamerule "+ key +" "+ value);
                        }
                    } catch (Exception e) {
                        sendServerCommand("gamerule "+ key +" "+ value);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Rewrite
    @Override
    public void togglePlugin(String fileName, boolean enabled) throws IOException, ActLaterException {
        Path pluginsPath = getPluginsPath();
        Path originalPath = pluginsPath.resolve(fileName);
        if(!Files.exists(originalPath)) {
            throw new NoSuchFileException("Plugin file not found: " + fileName);
        }

        final boolean isActuallyDisabled = fileName.endsWith(OPanelPlugin.DISABLED_SUFFIX);

        if(isActuallyDisabled && enabled) {
            // Rename from .jar.disabled to .jar
            Path newPath = pluginsPath.resolve(fileName.replaceAll("\\"+ OPanelPlugin.DISABLED_SUFFIX +"$", ""));
            try {
                Files.move(originalPath, newPath);
            } catch (Exception e) {
                FileOpsHelperApi.scheduleMove(originalPath.toString(), newPath.toString(), true);
                throw new ActLaterException();
            }
        } else if(!isActuallyDisabled && !enabled) {
            // Rename from .jar to .jar.disabled
            Path newPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
            try {
                Files.move(originalPath, newPath);
            } catch (Exception e) {
                FileOpsHelperApi.scheduleMove(originalPath.toString(), newPath.toString(), true);
                throw new ActLaterException();
            }
        } else if(!isActuallyDisabled) {
            // Cancel the pending operation of renaming from .jar to .jar.disabled
            Path targetPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
            FileOpsHelperApi.cancelPendingOperationsByTarget(List.of(targetPath.toString()));
            throw new ActLaterException();
        }
    }

    @Rewrite
    @Override
    public void deletePlugin(String fileName) throws IOException, ActLaterException {
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
            FileOpsHelperApi.scheduleDelete(List.of(filePath.toString()));
            throw new ActLaterException();
        }
    }

    @Override
    public OPanelChunkAccessor getChunkAccessor() {
        return chunkAccessor;
    }
}
