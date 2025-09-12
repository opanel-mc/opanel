package net.opanel.folia_1_20;

import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.Date;

public class FoliaPlayer implements OPanelPlayer {
    private final Main plugin;
    private final Player player;

    public FoliaPlayer(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
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
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> {
            switch(gamemode) {
                case ADVENTURE -> player.setGameMode(GameMode.ADVENTURE);
                case SURVIVAL -> player.setGameMode(GameMode.SURVIVAL);
                case CREATIVE -> player.setGameMode(GameMode.CREATIVE);
                case SPECTATOR -> player.setGameMode(GameMode.SPECTATOR);
            }
        }, null);
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> player.setOp(true), null);
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> player.setOp(false), null);
    }

    @Override
    public void kick(String reason) {
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> player.kickPlayer(reason), null);
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> {
            player.getServer().getBanList(BanList.Type.NAME).addBan(player.getName(), reason, null, null);
            player.kickPlayer(reason);
        }, null);
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