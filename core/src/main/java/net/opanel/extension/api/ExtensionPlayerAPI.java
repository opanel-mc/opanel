package net.opanel.extension.api;

import cn.opanel.api.exception.InvalidPlayerStateException;
import cn.opanel.api.player.GameMode;
import cn.opanel.api.player.InventoryAPI;
import cn.opanel.api.player.PlayerAPI;
import cn.opanel.api.player.Position;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.extension.ExtensionContext;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class ExtensionPlayerAPI implements PlayerAPI {
    private final ExtensionContext ctx;
    private final UUID uuid;
    private final InventoryAPI inventory;

    ExtensionPlayerAPI(ExtensionContext ctx, UUID uuid) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        inventory = new ExtensionInventoryAPI(ctx, uuid);
    }

    @Override
    public UUID getUuid() {
        ctx.ensureActive();
        return uuid;
    }

    @Override
    public String getName() {
        return ctx.call("get player name", () -> player().getName());
    }

    @Override
    public boolean isOnline() {
        return ctx.call("get player online status", () -> player().isOnline());
    }

    @Override
    public boolean isOp() {
        return ctx.call("get player operator status", () -> player().isOp());
    }

    @Override
    public boolean isBanned() {
        return ctx.call("get player ban status", () -> player().isBanned());
    }

    @Override
    public GameMode getGameMode() {
        return ctx.call("get player game mode", () -> (
            switch(player().getGameMode()) {
                case ADVENTURE -> GameMode.ADVENTURE;
                case SURVIVAL -> GameMode.SURVIVAL;
                case CREATIVE -> GameMode.CREATIVE;
                case SPECTATOR -> GameMode.SPECTATOR;
            }
        ));
    }

    @Override
    public Position getPosition() {
        return ctx.call("get player position", () -> {
            OPanelPlayer player = player();
            return new Position(player.getX(), player.getY(), player.getZ());
        });
    }

    @Override
    public InventoryAPI getInventory() {
        ctx.ensureActive();
        return inventory;
    }

    @Override
    public OptionalInt getPing() {
        return ctx.call("get player ping", () -> {
            OPanelPlayer player = player();
            return player.isOnline() ? OptionalInt.of(player.getPing()) : OptionalInt.empty();
        });
    }

    @Override
    public Optional<InetAddress> getAddress() {
        return ctx.call("get player address", () -> {
            OPanelPlayer player = player();
            return player.isOnline() ? Optional.ofNullable(player.getAddress()) : Optional.empty();
        });
    }

    @Override
    public Optional<String> getBanReason() {
        return ctx.call("get player ban reason", () -> {
            OPanelPlayer player = player();
            return player.isBanned() ? Optional.ofNullable(player.getBanReason()) : Optional.empty();
        });
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        Objects.requireNonNull(gameMode, "gameMode");
        ctx.run("set player game mode", () -> player().setGameMode(toCommonGameMode(gameMode)));
    }

    @Override
    public void setOp(boolean operator) {
        ctx.run("set player operator status", () -> {
            OPanelPlayer player = player();
            if(operator) player.giveOp();
            else player.depriveOp();
        });
    }

    @Override
    public void kick(String reason) {
        Objects.requireNonNull(reason, "reason");
        ctx.run("kick player", () -> {
            OPanelPlayer player = player();
            if(!player.isOnline()) {
                throw new InvalidPlayerStateException(uuid, "Cannot kick offline player " + uuid + ".");
            }
            player.kick(reason);
        });
    }

    @Override
    public void ban(String reason) {
        Objects.requireNonNull(reason, "reason");
        ctx.run("ban player", () -> player().ban(reason));
    }

    @Override
    public void pardon() {
        ctx.run("pardon player", () -> player().pardon());
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) return true;
        if(!(object instanceof ExtensionPlayerAPI player)) return false;
        return uuid.equals(player.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    private OPanelPlayer player() {
        return ctx.getPlayer(uuid);
    }

    private static OPanelGameMode toCommonGameMode(GameMode gameMode) {
        return switch(gameMode) {
            case ADVENTURE -> OPanelGameMode.ADVENTURE;
            case SURVIVAL -> OPanelGameMode.SURVIVAL;
            case CREATIVE -> OPanelGameMode.CREATIVE;
            case SPECTATOR -> OPanelGameMode.SPECTATOR;
        };
    }
}
