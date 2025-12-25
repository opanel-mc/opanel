package net.opanel.spigot_1_21_9;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.opanel.bukkit_helper.BaseBukkitServer;
import net.opanel.bukkit_helper.BukkitUtils;
import net.opanel.common.ServerType;
import net.opanel.common.*;
import net.opanel.common.features.BukkitConfigFeature;
import net.opanel.common.features.CodeOfConductFeature;
import net.opanel.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class SpigotServer extends BaseBukkitServer implements OPanelServer, CodeOfConductFeature, BukkitConfigFeature {
    public SpigotServer(Main plugin, Server server) {
        super(plugin, server);
    }

    @Override
    public ServerType getServerType() {
        if(Utils.hasClass("com.destroystokyo.paper.PaperConfig")) {
            return ServerType.PAPER;
        }
        if(Utils.hasClass("org.bukkit.entity.Player$Spigot")) {
            return ServerType.SPIGOT;
        }
        return ServerType.BUKKIT;
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
        server.setMotd(motd);
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
                        SpigotSave save = new SpigotSave((Main) plugin, server, path);
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
        return new SpigotSave((Main) plugin, server, savePath.toAbsolutePath());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        Collection<Player> players = (Collection<Player>) server.getOnlinePlayers();
        for(Player serverPlayer : players) {
            SpigotPlayer player = new SpigotPlayer((Main) plugin, serverPlayer);
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
                list.add(new SpigotPlayer((Main) plugin, serverPlayer));
            } else {
                list.add(new SpigotOfflinePlayer((Main) plugin, server, offlinePlayer));
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
        return new SpigotWhitelist((Main) plugin, server, server.getWhitelistedPlayers());
    }

    @Override
    public List<String> getCommandTabList(int argIndex, String command) {
        if(argIndex == 1) return getCommands();

        List<String> tabList = new ArrayList<>();
        String[] args = command.split(" ");

        try {
            CommandDispatcher<?> dispatcher = BukkitUtils.getCommandDispatcher(false);
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
    public HashMap<String, Object> getGamerules() {
        final World world = server.getWorlds().getFirst();
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
    public void setGamerules(HashMap<String, Object> gamerules) {
        HashMap<String, Object> currentGamerules = getGamerules();
        runner.runTask(() -> {
            final World world = server.getWorlds().getFirst();
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
}
