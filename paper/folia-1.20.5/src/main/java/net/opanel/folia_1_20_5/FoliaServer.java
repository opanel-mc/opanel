package net.opanel.folia_1_20_5;

import com.cozooo.dlc_fileops_helper.api.FileOpsHelperApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.kyori.adventure.text.Component;
import net.opanel.annotation.Rewrite;
import net.opanel.paper_helper.BasePaperServer;
import net.opanel.paper_helper.utils.PaperUtils;
import net.opanel.common.*;
import net.opanel.exception.MissingPlayerCacheException;
import net.opanel.common.features.PaperConfigFeature;
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
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class FoliaServer extends BasePaperServer implements OPanelServer, PaperConfigFeature {
    private final FoliaChunkAccessor chunkAccessor;

    public FoliaServer(Main plugin, Server server) {
        super(plugin, server);

        this.chunkAccessor = new FoliaChunkAccessor(plugin);
    }

    @Rewrite
    @Override
    public ServerType getServerType() {
        return ServerType.FOLIA;
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
                        FoliaSave save = new FoliaSave((Main) plugin, server, path);
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
        return new FoliaSave((Main) plugin, server, savePath.toAbsolutePath());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        Collection<Player> players = (Collection<Player>) server.getOnlinePlayers();
        for(Player serverPlayer : players) {
            FoliaPlayer player = new FoliaPlayer((Main) plugin, serverPlayer);
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
                list.add(new FoliaPlayer((Main) plugin, serverPlayer));
            } else {
                try {
                    list.add(new FoliaOfflinePlayer((Main) plugin, server, offlinePlayer));
                } catch (MissingPlayerCacheException e) {
                    //
                }
            }
        }
        return list;
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
        return new FoliaWhitelist((Main) plugin, server, server.getWhitelistedPlayers());
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
        for(String key : world.getGameRules()) {
            GameRule<?> rule = GameRule.getByName(key);
            if(rule == null) continue;
            gamerules.put(key, world.getGameRuleValue(rule));
        }
        return gamerules;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setGamerules(OPanelDimension dimension, HashMap<String, Object> gamerules) {
        HashMap<String, Object> currentGamerules = getGamerules(dimension);
        runner.runTask(() -> {
            final World world = PaperUtils.getWorldByDimension(dimension);
            gamerules.forEach((key, value) -> {
                if(value == null) return;
                final Object currentValue = currentGamerules.get(key);
                if(value.equals(currentValue)) return;
                GameRule<?> rule = GameRule.getByName(key);
                if(rule == null) return;

                if(rule.getType().equals(Boolean.class)) { // boolean
                    world.setGameRule((GameRule<Boolean>) rule, (Boolean) value);
                } else if(rule.getType().equals(Integer.class)) { // integer
                    int n = ((Number) value).intValue();
                    world.setGameRule((GameRule<Integer>) rule, n);
                } else { // string
                    sendServerCommand("gamerule "+ key +" "+ value);
                }
            });
        });
    }

    @Rewrite
    @Override
    public void reload() {
        throw new UnsupportedOperationException("Folia doesn't support reload operation");
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

    @Rewrite
    @Override
    public void updatePlugin(String fileName, Path newPluginFile) throws IOException, ActLaterException {
        Path pluginsPath = getPluginsPath();
        Path targetPath = pluginsPath.resolve(fileName);
        if(!Files.exists(targetPath)) {
            targetPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        }

        if(!Files.exists(targetPath)) {
            throw new NoSuchFileException("Plugin file not found: " + fileName);
        }

        try {
            Files.move(newPluginFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // The plugin is loaded and cannot be overwritten, defer it to the server restart
            Path deferredPath = pluginsPath.resolve(fileName + ".update");
            try {
                Files.move(newPluginFile, deferredPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e2) {
                throw new IllegalStateException("Cannot update a loaded plugin.");
            }
            FileOpsHelperApi.cancelPendingOperationsByTarget(List.of(targetPath.toString()));
            FileOpsHelperApi.scheduleMove(deferredPath.toString(), targetPath.toString(), true);
            throw new ActLaterException();
        }
    }

    @Override
    public OPanelChunkAccessor getChunkAccessor() {
        return chunkAccessor;
    }
}
