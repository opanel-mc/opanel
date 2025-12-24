package net.opanel.fabric_1_19_4;

import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.BannedIpEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelWhitelist;
import net.opanel.fabric_helper.BaseFabricServer;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class FabricServer extends BaseFabricServer implements OPanelServer {
    public FabricServer(MinecraftServer server) {
        super(server);
    }

    @Override
    public byte[] getFavicon() {
        byte[] serverIconPNG = super.getFavicon();
        if(serverIconPNG != null) return serverIconPNG;

        ServerMetadata metadata = server.getServerMetadata();
        if(metadata == null) return null;

        Optional<ServerMetadata.Favicon> faviconOptional = metadata.favicon();
        if(faviconOptional.isEmpty()) return null;

        ServerMetadata.Favicon favicon = faviconOptional.get();
        return favicon.iconBytes();
    }

    @Override
    public void setFavicon(byte[] iconBytes) throws IOException {
        super.setFavicon(iconBytes);
        // reload server favicon
        ServerMetadata metadata = server.getServerMetadata();
        ServerMetadata.Favicon favicon = new ServerMetadata.Favicon(iconBytes);
        ServerMetadata newStatus = new ServerMetadata(
                metadata.description(),
                metadata.players(),
                metadata.version(),
                Optional.of(favicon),
                metadata.secureChatEnforced()
        );
        try {
            Field faviconField = MinecraftServer.class.getDeclaredField("field_42958"); // field_42958 -> favicon
            faviconField.setAccessible(true);
            faviconField.set(server, favicon);

            Field metadataField = MinecraftServer.class.getDeclaredField("field_4593"); // field_4593 -> metadata
            metadataField.setAccessible(true);
            metadataField.set(server, newStatus);
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
                        FabricSave save = new FabricSave(server, path);
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
        return new FabricSave(server, savePath.toAbsolutePath());
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
                        try {
                            final String uuid = item.getFileName().toString().replace(".dat", "");
                            ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
                            if(serverPlayer != null && !serverPlayer.isDisconnected()) return;

                            FabricOfflinePlayer player = new FabricOfflinePlayer(server, UUID.fromString(uuid));
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
    public List<String> getBannedIps() {
        Collection<BannedIpEntry> entries = server.getPlayerManager().getIpBanList().values();
        List<String> list = new ArrayList<>();
        entries.forEach(entry -> list.add(entry.toText().getString()));
        return list;
    }

    @Override
    public void banIp(String ip) {
        if(getBannedIps().contains(ip)) return;
        server.getPlayerManager().getIpBanList().add(new BannedIpEntry(ip));
    }

    @Override
    public void pardonIp(String ip) {
        if(!getBannedIps().contains(ip)) return;
        server.getPlayerManager().getIpBanList().remove(ip);
    }

    @Override
    public boolean isWhitelistEnabled() {
        return server.getPlayerManager().isWhitelistEnabled();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        server.getPlayerManager().setWhitelistEnabled(enabled);
    }

    @Override
    public OPanelWhitelist getWhitelist() {
        return new FabricWhitelist(server.getPlayerManager().getWhitelist());
    }

    @Override
    public void sendServerCommand(String command) {
        CommandManager manager = server.getCommandManager();
        ServerCommandSource source = server.getCommandSource();
        manager.executeWithPrefix(source, command);
    }

    @Override
    public HashMap<String, Object> getGamerules() {
        final NbtCompound gamerulesNbt = server.getGameRules().toNbt();
        HashMap<String, Object> gamerules = new HashMap<>();
        for(String key : gamerulesNbt.getKeys()) {
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
        GameRules.accept(new GameRules.Visitor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                GameRules.Visitor.super.visit(key, type);

                final String ruleName = key.getName();
                final Object value = gamerules.get(ruleName);
                if(value == null) return;
                final Object currentValue = currentGamerules.get(ruleName);
                if(value.equals(currentValue)) return;

                T rule = type.createRule();
                if(rule instanceof GameRules.BooleanRule) { // boolean
                    ((GameRules.BooleanRule) rule).set((boolean) value, server);
                    gameRulesObj.get(key).setValue(rule, server);
                } else if(rule instanceof GameRules.IntRule) { // integer
                    int n = ((Number) value).intValue();
                    ((GameRules.IntRule) rule).set(n, server);
                    gameRulesObj.get(key).setValue(rule, server);
                } else if(rule instanceof DoubleRule || value instanceof String) { // double, enum, string
                    sendServerCommand("gamerule "+ ruleName +" "+ value);
                }
            }
        });
    }
}
