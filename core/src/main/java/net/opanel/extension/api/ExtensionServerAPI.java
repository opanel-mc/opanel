package net.opanel.extension.api;

import net.opanel.api.PlayerAPI;
import net.opanel.api.ServerAPI;
import net.opanel.api.ServerType;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    @FunctionalInterface
    private interface PlayerListSupplier {
        List<OPanelPlayer> get(OPanelServer server);
    }
}
