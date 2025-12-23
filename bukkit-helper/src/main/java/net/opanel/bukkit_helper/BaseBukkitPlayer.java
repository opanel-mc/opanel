package net.opanel.bukkit_helper;

import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class BaseBukkitPlayer implements OPanelPlayer {
    protected final JavaPlugin plugin;
    protected final TaskRunner runner;
    protected final Player player;

    public BaseBukkitPlayer(JavaPlugin plugin, Player player) {
        this.plugin = plugin;
        runner = (TaskRunner) plugin;
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
        runner.runTask(() -> {
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
        runner.runTask(() -> player.setOp(true));
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        runner.runTask(() -> player.setOp(false));
    }

    @Override
    public String getBanReason() {
        return null;
    }

    @Override
    public void pardon() { }
}
