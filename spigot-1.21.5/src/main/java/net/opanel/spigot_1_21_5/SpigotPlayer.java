package net.opanel.spigot_1_21_5;

import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.util.Date;

public class SpigotPlayer implements OPanelPlayer {
    private final Main plugin;
    private final Player player;
    private final Server server;
    private final PlayerProfile profile;

    public SpigotPlayer(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        server = player.getServer();
        profile = player.getPlayerProfile();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getUUID() {
        return player.getUniqueId().toString();
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public boolean isOp() {
        return player.isOp();
    }

    @Override
    public boolean isBanned() {
        return false;
    }

    @Override
    public OPanelGameMode getGameMode() {
        GameMode gamemode = player.getGameMode();
        switch(gamemode) {
            case ADVENTURE -> { return OPanelGameMode.ADVENTURE; }
            case SURVIVAL -> { return OPanelGameMode.SURVIVAL; }
            case CREATIVE -> { return OPanelGameMode.CREATIVE; }
            case SPECTATOR -> { return OPanelGameMode.SPECTATOR; }
        }
        return null;
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        plugin.runTask(() -> {
            switch(gamemode) {
                case ADVENTURE -> player.setGameMode(GameMode.ADVENTURE);
                case SURVIVAL -> player.setGameMode(GameMode.SURVIVAL);
                case CREATIVE -> player.setGameMode(GameMode.CREATIVE);
                case SPECTATOR -> player.setGameMode(GameMode.SPECTATOR);
            }
        });
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        plugin.runTask(() -> player.setOp(true));
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        plugin.runTask(() -> player.setOp(false));
    }

    @Override
    public void kick(String reason) {
        plugin.runTask(() -> player.kickPlayer(reason));
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        plugin.runTask(() -> player.ban(reason, (Date) null, null, true));
    }

    @Override
    public String getBanReason() {
        return null;
    }

    @Override
    public void pardon() { }

    @Override
    public int getPing() {
        return player.getPing();
    }
}
