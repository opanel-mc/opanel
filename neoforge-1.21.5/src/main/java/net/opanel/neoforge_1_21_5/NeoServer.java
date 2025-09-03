package net.opanel.neoforge_1_21_5;

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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.ServerType;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
        return ServerType.FORGE;
    }

    @Override
    public byte[] getFavicon() {
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
        OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("motd=.+", "motd="+ motd));
        // Directly modify motd in memory
        final DedicatedServerProperties properties = dedicatedServer.getProperties();
        try {
            // Force modifying motd field through reflect because it is final
            Field motdField = properties.getClass().getDeclaredField("motd");
            motdField.setAccessible(true);
            motdField.set(properties, motd);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        final Path savePath = Paths.get("").resolve(saveName);
        if(!Files.exists(savePath) || !Files.exists(savePath.resolve("level.dat"))) {
            return null;
        }
        return new NeoSave(server, savePath.toAbsolutePath());
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
                        final String uuid = item.getFileName().toString().replace(".dat", "");
                        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(UUID.fromString(uuid));
                        if(serverPlayer != null && !serverPlayer.hasDisconnected()) return;

                        try {
                            NeoOfflinePlayer player = new NeoOfflinePlayer(server, UUID.fromString(uuid));
                            list.add(player);
                        } catch (NullPointerException e) {
                            //
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
    public HashMap<String, Object> getGamerules() {
        final CompoundTag gamerulesNbt = server.getGameRules().createTag();
        HashMap<String, Object> gamerules = new HashMap<>();
        for(String key : gamerulesNbt.keySet()) {
            final String valueStr = gamerulesNbt.getStringOr(key, "");
            if(valueStr.equals("true") || valueStr.equals("false")) {
                gamerules.put(key, Boolean.valueOf(valueStr));
            } else {
                gamerules.put(key, Integer.valueOf(valueStr));
            }
        }
        return gamerules;
    }

    @Override
    public void setGamerules(HashMap<String, Object> gamerules) {
        final GameRules gameRulesObj = server.getGameRules();
        gameRulesObj.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                GameRules.GameRuleTypeVisitor.super.visit(key, type);
                gamerules.forEach((ruleName, value) -> {
                    if(value == null) return;
                    if(key.getId().equals(ruleName)) {
                        if(value instanceof Boolean) {
                            gameRulesObj.getRule(key).setFrom((T) new GameRules.BooleanValue((GameRules.Type<GameRules.BooleanValue>) type, (boolean) value), server);
                        } else if(value instanceof Number) {
                            gameRulesObj.getRule(key).setFrom((T) new GameRules.IntegerValue((GameRules.Type<GameRules.IntegerValue>) type, Double.valueOf((double) value).intValue()), server);
                        }
                    }
                });
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
