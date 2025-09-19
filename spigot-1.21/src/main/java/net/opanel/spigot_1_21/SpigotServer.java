package net.opanel.spigot_1_21;

import net.opanel.ServerType;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class SpigotServer implements OPanelServer {
    private static final Path serverIconPath = Paths.get("").resolve("server-icon.png");

    private final Main plugin;
    private final Server server;

    public SpigotServer(Main plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.BUKKIT;
    }

    @Override
    public byte[] getFavicon() {
        if(!Files.exists(serverIconPath)) return null;
        try {
            return Files.readAllBytes(serverIconPath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
        OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("motd=.+", "motd="+ motd));
    }

    @Override
    public String getVersion() {
        return server.getBukkitVersion();
    }

    @Override
    public int getPort() {
        return server.getPort();
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
                        SpigotSave save = new SpigotSave(server, path);
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
        return new SpigotSave(server, savePath.toAbsolutePath());
    }

    @Override
    public void saveAll() {
        plugin.runTask(() -> {
            for(World world : server.getWorlds()) {
                world.save();
            }
            server.savePlayers();
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        Collection<Player> players = (Collection<Player>) server.getOnlinePlayers();
        for(Player serverPlayer : players) {
            SpigotPlayer player = new SpigotPlayer(plugin, serverPlayer);
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
                list.add(new SpigotPlayer(plugin, serverPlayer));
            } else {
                list.add(new SpigotOfflinePlayer(plugin, server, offlinePlayer));
            }
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
    public boolean isWhitelistEnabled() {
        return server.hasWhitelist();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        server.setWhitelist(enabled);
    }

    @Override
    public OPanelWhitelist getWhitelist() {
        return new SpigotWhitelist(plugin, server, server.getWhitelistedPlayers());
    }

    @Override
    public void sendServerCommand(String command) {
        plugin.runTask(() -> Bukkit.dispatchCommand(server.getConsoleSender(), command));
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        Set<String> uniqueCommands = new HashSet<>();
        
        // Get commands from HelpMap (includes plugin commands)
        for(HelpTopic topic : server.getHelpMap().getHelpTopics()) {
            String commandName = topic.getName().toLowerCase().replaceFirst("/", "");
            if (!commandName.isEmpty() && !commandName.contains(" ")) {
                uniqueCommands.add(commandName);
            }
        }
        
        // Add common vanilla commands that might be missing from HelpMap
        String[] commonCommands = {
            "give", "tp", "teleport", "gamemode", "time", "weather", 
            "kill", "clear", "effect", "enchant", "experience", "xp",
            "fill", "setblock", "summon", "say", "tell", "msg", "list",
            "op", "deop", "ban", "unban", "pardon", "kick", "whitelist",
            "reload", "stop", "save-all", "difficulty", "gamerule"
        };
        
        for (String cmd : commonCommands) {
            uniqueCommands.add(cmd);
        }
        
        // Filter out common unwanted commands
        uniqueCommands.removeIf(cmd -> 
            cmd.startsWith("minecraft:") || 
            cmd.startsWith("bukkit:") ||
            cmd.equals("?") ||
            cmd.length() > 50 // Remove extremely long command names
        );
        
        commands.addAll(uniqueCommands);
        commands.sort(String::compareToIgnoreCase);
        return commands;
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
        plugin.runTask(() -> {
            final World world = server.getWorlds().getFirst();
            gamerules.forEach((key, value) -> {
                if(value == null) return;
                GameRule<?> rule = GameRule.getByName(key);
                if(rule == null) return;
                if(value instanceof Boolean) {
                    world.setGameRule((GameRule<Boolean>) rule, (Boolean) value);
                } else if(value instanceof Number) {
                    world.setGameRule((GameRule<Integer>) rule, Double.valueOf((double) value).intValue());
                }
            });
        });
    }

    @Override
    public void reload() {
        if(Main.isPaper) {
            sendServerCommand("reload confirm");
        } else {
            sendServerCommand("reload");
        }
    }

    @Override
    public void stop() {
        server.shutdown();
    }

    @Override
    public long getIngameTime() {
        return server.getWorlds().getFirst().getTime();
    }
}
