package net.opanel.fabric_1_21_5;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.gamerule.v1.FabricGameRuleVisitor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class FabricServer implements OPanelServer {
    private final MinecraftServer server;

    public FabricServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public byte[] getFavicon() {
        ServerMetadata metadata = server.getServerMetadata();
        if(metadata == null) return null;

        Optional<ServerMetadata.Favicon> faviconOptional = metadata.favicon();
        if(faviconOptional.isEmpty()) return null;

        ServerMetadata.Favicon favicon = faviconOptional.get();
        return favicon.iconBytes();
    }

    @Override
    public String getMotd() {
        return server.getServerMotd();
    }

    @Override
    public String getVersion() {
        return server.getVersion();
    }

    @Override
    public int getPort() {
        return server.getServerPort();
    }

    @Override
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for(ServerPlayerEntity serverPlayer : players) {
            FabricPlayer player = new FabricPlayer(serverPlayer);
            list.add(player);
        }
        return list;
    }

    @Override
    public List<OPanelPlayer> getPlayers() {
        final Path playerDataPath = server.getSavePath(WorldSavePath.PLAYERDATA);
        // load online players
        List<OPanelPlayer> list = new ArrayList<>(getOnlinePlayers());

        // load offline players
        try(Stream<Path> stream = Files.list(playerDataPath)) {
            stream.filter(item -> !Files.isDirectory(item) && item.toString().endsWith(".dat"))
                    .forEach(item -> {
                        final String uuid = item.getFileName().toString().replace(".dat", "");
                        ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
                        if(serverPlayer != null && !serverPlayer.isDisconnected()) return;

                        FabricOfflinePlayer player = new FabricOfflinePlayer(server, item, UUID.fromString(uuid));
                        list.add(player);
                    });
        } catch (IOException e) {
            e.printStackTrace();
            return list;
        }
        return list;
    }

    @Override
    public int getMaxPlayerCount() {
        return server.getMaxPlayerCount();
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
    public void sendServerCommand(String command) {
        CommandManager manager = server.getCommandManager();
        ServerCommandSource source = server.getCommandSource();
        manager.executeWithPrefix(source, command);
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        for(CommandNode<ServerCommandSource> node : dispatcher.getRoot().getChildren()) {
            commands.add(node.getName());
        }
        return commands;
    }

    @Override
    public HashMap<String, Object> getGamerules() {
        final NbtCompound gamerulesNbt = server.getGameRules().toNbt();
        HashMap<String, Object> gamerules = new HashMap<>();
        for(String key : gamerulesNbt.getKeys()) {
            final String valueStr = gamerulesNbt.getString(key, "");
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
        gameRulesObj.accept(new GameRules.Visitor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                GameRules.Visitor.super.visit(key, type);
                gamerules.forEach((ruleName, value) -> {
                    if(key.getName().equals(ruleName)) {
                        if(value instanceof Boolean) {
                            gameRulesObj.get(key).setValue((T) new GameRules.BooleanRule((GameRules.Type<GameRules.BooleanRule>) type, (boolean) value), server);
                        } else if(value instanceof Number) {
                            gameRulesObj.get(key).setValue((T) new GameRules.IntRule((GameRules.Type<GameRules.IntRule>) type, Double.valueOf((double) value).intValue()), server);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void reload() {
        // directly execute /reload
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), "reload");
    }

    @Override
    public void stop() {
        server.stop(false);
    }
}
