package net.opanel.forge_1_20_1;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.ServerType;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class ForgeServer implements OPanelServer {
    private final MinecraftServer server;
    private final DedicatedServer dedicatedServer;

    public ForgeServer(MinecraftServer server) {
        this.server = server;
        dedicatedServer = (DedicatedServer) server;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.FORGE;
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
    public void saveAll() {
        server.saveEverything(true, true, true);
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
        return new ForgeWhitelist(server.getPlayerList().getWhiteList());
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
}
