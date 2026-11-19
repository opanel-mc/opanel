package net.opanel.paper_helper;

import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelPlayerPosition;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public abstract class BasePaperPlayer implements OPanelPlayer {
    protected final JavaPlugin plugin;
    protected final TaskRunner runner;
    protected final Player player;

    public BasePaperPlayer(JavaPlugin plugin, Player player) {
        this.plugin = plugin;
        runner = (TaskRunner) plugin;
        this.player = player;
    }

    @Override
    public OPanelPlayerPosition getPosition() {
        return toPosition(player.getLocation());
    }

    public OPanelPlayerPosition toPosition(Location location) {
        if(location == null) {
            return new OPanelPlayerPosition(getX(), getY(), getZ(), null);
        }
        World world = location.getWorld();
        String save = (world != null && isMapWorld(world)) ? world.getName() : null;
        return new OPanelPlayerPosition(location.getX(), location.getY(), location.getZ(), save);
    }

    private boolean isMapWorld(World world) {
        return !plugin.getServer().getWorlds().isEmpty()
                && world.equals(plugin.getServer().getWorlds().get(0));
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
    public double getX() {
        return player.getLocation().getX();
    }

    @Override
    public double getY() {
        return player.getLocation().getY();
    }

    @Override
    public double getZ() {
        return player.getLocation().getZ();
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

    @Override
    public InetAddress getAddress() {
        InetSocketAddress socketAddress = player.getAddress();
        return socketAddress == null ? null : socketAddress.getAddress();
    }
}
