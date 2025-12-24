package net.opanel.forge_1_21_11;

import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.*;
import net.opanel.common.features.CodeOfConductFeature;
import net.opanel.forge_helper.BaseForgeServer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ForgeServer extends BaseForgeServer implements OPanelServer, CodeOfConductFeature {
    public ForgeServer(MinecraftServer server) {
        super(server);
    }

    @Override
    public byte[] getFavicon() {
        byte[] serverIconPNG = super.getFavicon();
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
                status.forgeData()
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
    public List<OPanelPlayer> getOnlinePlayers() {
        List<OPanelPlayer> list = new ArrayList<>();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for(ServerPlayer serverPlayer : players) {
            ForgePlayer player = new ForgePlayer(serverPlayer, server);
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
    public List<String> getBannedIps() {
        Collection<IpBanListEntry> entries = server.getPlayerList().getIpBans().getEntries();
        List<String> list = new ArrayList<>();
        entries.forEach(entry -> list.add(entry.getUser()));
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
        server.setUsingWhitelist(enabled);
    }

    @Override
    public OPanelWhitelist getWhitelist() {
        return new ForgeWhitelist(server.getPlayerList().getWhiteList());
    }

    @Override
    public HashMap<String, Object> getGamerules() {
        HashMap<String, Object> gamerules = new HashMap<>();
        final GameRules gameRulesObj = server.overworld().getGameRules();
        gameRulesObj.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(GameRule<T> rule) {
                GameRuleTypeVisitor.super.visit(rule);

                final String ruleName = rule.getIdentifier().toShortString();
                final T value = gameRulesObj.get(rule);
                gamerules.put(ruleName, value);
            }
        });
        return gamerules;
    }

    @Override
    public void setGamerules(HashMap<String, Object> gamerules) {
        HashMap<String, Object> currentGamerules = getGamerules();
        final GameRules gameRulesObj = server.overworld().getGameRules();
        gameRulesObj.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> void visit(GameRule<T> rule) {
                GameRuleTypeVisitor.super.visit(rule);

                final GameRuleType type = rule.gameRuleType();
                final String ruleName = rule.getIdentifier().toShortString();
                final T value = (T) gamerules.get(ruleName);

                final Object currentValue = currentGamerules.get(ruleName);
                if(value.equals(currentValue)) return;

                try {
                    if(type == GameRuleType.INT) {
                        Integer n = ((Number) value).intValue();
                        gameRulesObj.set(rule, (T) n, server);
                    } else if(value instanceof Double) {
                        Double n = ((Number) value).doubleValue();
                        gameRulesObj.set(rule, (T) n, server);
                    } else {
                        gameRulesObj.set(rule, value, server);
                    }
                } catch (Exception e) {
                    sendServerCommand("gamerule "+ ruleName +" "+ value);
                }
            }
        });
    }
}
