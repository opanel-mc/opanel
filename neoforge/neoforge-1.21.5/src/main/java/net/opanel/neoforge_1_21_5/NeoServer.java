package net.opanel.neoforge_1_21_5;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.*;
import net.opanel.exception.MissingPlayerCacheException;
import net.opanel.neoforge_helper.BaseNeoServer;
import net.opanel.neoforge_helper.utils.NeoUtils;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NeoServer extends BaseNeoServer implements OPanelServer {
    private final NeoChunkAccessor chunkAccessor;

    public NeoServer(MinecraftServer server) {
        super(server);

        chunkAccessor = new NeoChunkAccessor(server);
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
    public OPanelPlayer getPlayer(String uuid) {
        for(OPanelPlayer player : getPlayers()) {
            if(player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public OPanelWhitelist getWhitelist() {
        return new NeoWhitelist(server.getPlayerList().getWhiteList());
    }

    @Override
    public HashMap<String, Object> getGamerules(OPanelDimension dimension) {
        final CompoundTag gamerulesNbt = NeoUtils.getLevelByDimension(server, dimension).getGameRules().createTag();
        HashMap<String, Object> gamerules = new HashMap<>();
        for(String key : gamerulesNbt.keySet()) {
            final String valueStr = gamerulesNbt.getStringOr(key, "");
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
        final GameRules gameRulesObj = NeoUtils.getLevelByDimension(server, dimension).getGameRules();
        gameRulesObj.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
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
    public OPanelChunkAccessor getChunkAccessor() {
        return chunkAccessor;
    }
}

