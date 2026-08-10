package net.opanel.extension.api;

import cn.opanel.api.player.PlayerAPI;
import cn.opanel.api.server.Dimension;
import cn.opanel.api.server.ServerAPI;
import cn.opanel.api.server.ServerType;
import net.opanel.common.OPanelDimension;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.extension.ExtensionContext;

import java.util.*;

public final class ExtensionServerAPI implements ServerAPI {
    private final ExtensionContext ctx;

    ExtensionServerAPI(ExtensionContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public ServerType getServerType() {
        return ctx.call("get server type", () -> (
            switch(ctx.getServer().getServerType()) {
                case PAPER -> ServerType.PAPER;
                case FABRIC -> ServerType.FABRIC;
                case FORGE -> ServerType.FORGE;
                case NEOFORGE -> ServerType.NEOFORGE;
                case FOLIA -> ServerType.FOLIA;
                case LEAVES -> ServerType.LEAVES;
            }
        ));
    }

    @Override
    public String getMinecraftVersion() {
        return ctx.call("get Minecraft version", () -> ctx.getServer().getVersion());
    }

    @Override
    public String getMotd() {
        return ctx.call("get server MOTD", () -> ctx.getServer().getMotd());
    }

    @Override
    public void setMotd(String motd) {
        Objects.requireNonNull(motd, "motd");
        ctx.run("set server MOTD", () -> ctx.getServer().setMotd(motd));
    }

    @Override
    public int getPort() {
        return ctx.call("get server port", () -> ctx.getServer().getPort());
    }

    @Override
    public int getMaxPlayerCount() {
        return ctx.call("get maximum player count", () -> ctx.getServer().getMaxPlayerCount());
    }

    @Override
    public List<PlayerAPI> getOnlinePlayers() {
        return getPlayerSnapshot("get online players", OPanelServer::getOnlinePlayers);
    }

    @Override
    public List<PlayerAPI> getPlayers() {
        return getPlayerSnapshot("get players", OPanelServer::getPlayers);
    }

    @Override
    public Optional<PlayerAPI> getPlayer(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return ctx.call("get player", () -> (
                ctx.getServer().getPlayer(uuid.toString())) == null
                ? Optional.empty()
                : Optional.of(new ExtensionPlayerAPI(ctx, uuid)
        ));
    }

    @Override
    public Map<String, Object> getGamerules(Dimension dimension) {
        Objects.requireNonNull(dimension, "dimension");
        return ctx.call("get game rules", () -> Collections.unmodifiableMap(
                new LinkedHashMap<>(ctx.getServer().getGamerules(toCommonDimension(dimension)))
        ));
    }

    @Override
    public void setGamerules(Dimension dimension, Map<String, Object> gamerules) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(gamerules, "gamerules");

        Map<String, Object> updates = new LinkedHashMap<>();
        for(Map.Entry<String, Object> entry : gamerules.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), "gamerule key");
            if(key.isBlank()) throw new IllegalArgumentException("gamerule key must not be blank");
            updates.put(key, Objects.requireNonNull(entry.getValue(), "gamerule value"));
        }

        ctx.run("set game rules", () -> {
            OPanelDimension commonDimension = toCommonDimension(dimension);
            HashMap<String, Object> merged = new HashMap<>(ctx.getServer().getGamerules(commonDimension));
            merged.putAll(updates);
            ctx.getServer().setGamerules(commonDimension, merged);
        });
    }

    @Override
    public void setGamerule(Dimension dimension, String key, Object value) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if(key.isBlank()) throw new IllegalArgumentException("key must not be blank");

        ctx.run("set game rule", () -> {
            OPanelDimension commonDimension = toCommonDimension(dimension);
            HashMap<String, Object> gamerules = ctx.getServer().getGamerules(commonDimension);
            if(!gamerules.containsKey(key)) {
                throw new IllegalArgumentException("Unknown game rule: " + key);
            }
            gamerules.put(key, value);
            ctx.getServer().setGamerules(commonDimension, gamerules);
        });
    }

    @Override
    public boolean isWhitelistEnabled() {
        return ctx.call("get whitelist status", () -> ctx.getServer().isWhitelistEnabled());
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        ctx.run("set whitelist status", () -> ctx.getServer().setWhitelistEnabled(enabled));
    }

    @Override
    public void saveAll() {
        ctx.run("save all server data", () -> ctx.getServer().saveAll());
    }

    @Override
    public void sendServerCommand(String command) {
        Objects.requireNonNull(command, "command");
        if(command.isBlank()) throw new IllegalArgumentException("command must not be blank");
        ctx.run("dispatch server command", () -> ctx.getServer().sendServerCommand(command));
    }

    private List<PlayerAPI> getPlayerSnapshot(String operation, PlayerListSupplier supplier) {
        return ctx.call(operation, () -> {
            List<PlayerAPI> players = new ArrayList<>();
            for(OPanelPlayer player : supplier.get(ctx.getServer())) {
                players.add(new ExtensionPlayerAPI(ctx, UUID.fromString(player.getUUID())));
            }
            return Collections.unmodifiableList(players);
        });
    }

    private static OPanelDimension toCommonDimension(Dimension dimension) {
        return switch(dimension) {
            case OVERWORLD -> OPanelDimension.OVERWORLD;
            case NETHER -> OPanelDimension.NETHER;
            case THE_END -> OPanelDimension.THE_END;
        };
    }

    @FunctionalInterface
    private interface PlayerListSupplier {
        List<OPanelPlayer> get(OPanelServer server);
    }
}
