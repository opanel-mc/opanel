package net.opanel.bukkit_1_21_5;

import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;
import net.opanel.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.CachedServerIcon;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class BukkitServer implements OPanelServer {
    private static final Path serverPropertiesPath = Paths.get("").resolve("server.properties");

    private final Server server;

    public BukkitServer(Server server) {
        this.server = server;
    }

    @Override
    public byte[] getFavicon() {
        CachedServerIcon icon = server.getServerIcon();
        if(icon == null) return null;

        String iconData = icon.getData();
        if(iconData == null) return null;

        try {
            return iconData.getBytes(StandardCharsets.UTF_8);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getMotd() {
        return LegacyComponentSerializer.legacySection().serialize(server.motd());
    }

    @Override
    public void setMotd(String motd) throws IOException {
        // setMotd() alternative
        server.motd(LegacyComponentSerializer.legacySection().deserialize(motd));
        // Directly modify motd in server.properties
        Properties properties = new Properties();
        properties.load(new FileInputStream(serverPropertiesPath.toFile()));
        properties.setProperty("motd", motd);
        writePropertiesContent(properties.toString());
    }

    @Override
    public String getVersion() {
        return server.getVersion();
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
                            Files.exists(path.resolve("level.dat"))
                                    && !Files.isDirectory(path.resolve("level.dat"))
                    ))
                    .map(Path::toAbsolutePath)
                    .forEach(path -> {
                        BukkitSave save = new BukkitSave(server, path);
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
        return new BukkitSave(server, savePath.toAbsolutePath());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        Collection<Player> players = (Collection<Player>) server.getOnlinePlayers();
        for(Player serverPlayer : players) {
            BukkitPlayer player = new BukkitPlayer(serverPlayer);
            list.add(player);
        }
        return list;
    }

    @Override
    public List<OPanelPlayer> getPlayers() {
        List<OPanelPlayer> list = new ArrayList<>(getOnlinePlayers());
        OfflinePlayer[] players = server.getOfflinePlayers();
        for(OfflinePlayer offlinePlayer : players) {
            Player serverPlayer = offlinePlayer.getPlayer();
            if(serverPlayer == null) continue;
            BukkitPlayer player = new BukkitPlayer(serverPlayer);
            list.add(player);
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
        return new BukkitWhitelist(server, server.getWhitelistedPlayers());
    }

    @Override
    public void sendServerCommand(String command) {
        server.getCommandMap().dispatch(server.getConsoleSender(), command);
    }

    @Override
    public List<String> getCommands() {
        return new ArrayList<>(server.getCommandMap().getKnownCommands().keySet());
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
        final World world = server.getWorlds().getFirst();
        gamerules.forEach((key, value) -> {
            GameRule<?> rule = GameRule.getByName(key);
            if(rule == null) return;
            if(value instanceof Boolean) {
                world.setGameRule((GameRule<Boolean>) rule, (Boolean) value);
            } else if(value instanceof Number) {
                world.setGameRule((GameRule<Integer>) rule, Double.valueOf((double) value).intValue());
            }
        });
    }

    @Override
    public void reload() {
        server.reload();
    }

    @Override
    public void stop() {
        server.shutdown();
    }

    @Override
    public String getPropertiesContent() throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        return Utils.readTextFile(serverPropertiesPath);
    }

    @Override
    public void writePropertiesContent(String newContent) throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        Utils.writeTextFile(serverPropertiesPath, newContent);
    }

    @Override
    public long getIngameTime() {
        return server.getWorlds().getFirst().getGameTime();
    }
}
