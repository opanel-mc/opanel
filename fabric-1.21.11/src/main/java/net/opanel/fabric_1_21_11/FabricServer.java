package net.opanel.fabric_1_21_11;

import net.minecraft.server.BannedIpEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleType;
import net.minecraft.world.rule.GameRuleVisitor;
import net.minecraft.world.rule.GameRules;
import net.opanel.annotation.Rewrite;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelWhitelist;
import net.opanel.common.features.CodeOfConductFeature;
import net.opanel.fabric_helper.BaseFabricServer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class FabricServer extends BaseFabricServer implements OPanelServer, CodeOfConductFeature {
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
            FabricPlayer player = new FabricPlayer(serverPlayer, server);
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
        entries.forEach(entry -> list.add(entry.getKey()));
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
        server.setUseAllowlist(enabled);
    }

    @Override
    public OPanelWhitelist getWhitelist() {
        return new FabricWhitelist(server.getPlayerManager().getWhitelist());
    }

    @Rewrite
    @Override
    public void sendServerCommand(String command) {
        CommandManager manager = server.getCommandManager();
        ServerCommandSource source = server.getCommandSource();
        manager.parseAndExecute(source, command);
    }

    @Override
    public HashMap<String, Object> getGamerules() {
        HashMap<String, Object> gamerules = new HashMap<>();
        final GameRules gameRulesObj = server.getSpawnWorld().getGameRules();
        gameRulesObj.accept(new GameRuleVisitor() {
            @Override
            public <T> void visit(GameRule<T> rule) {
                GameRuleVisitor.super.visit(rule);

                final String ruleName = rule.getId().toShortString();
                final T value = gameRulesObj.getValue(rule);
                gamerules.put(ruleName, value);
            }
        });
        return gamerules;
    }

    @Override
    public void setGamerules(HashMap<String, Object> gamerules) {
        HashMap<String, Object> currentGamerules = getGamerules();
        final GameRules gameRulesObj = server.getSpawnWorld().getGameRules();
        gameRulesObj.accept(new GameRuleVisitor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> void visit(GameRule<T> rule) {
                GameRuleVisitor.super.visit(rule);

                final GameRuleType type = rule.getType();
                final String ruleName = rule.getId().toShortString();
                final T value = (T) gamerules.get(ruleName);

                final Object currentValue = currentGamerules.get(ruleName);
                if(value.equals(currentValue)) return;

                try {
                    if(type == GameRuleType.INT) {
                        Integer n = ((Number) value).intValue();
                        gameRulesObj.setValue(rule, (T) n, server);
                    } else if(value instanceof Double) {
                        Double n = ((Number) value).doubleValue();
                        gameRulesObj.setValue(rule, (T) n, server);
                    } else {
                        gameRulesObj.setValue(rule, value, server);
                    }
                } catch (Exception e) {
                    sendServerCommand("gamerule "+ ruleName +" "+ value);
                }
            }
        });
    }
}
