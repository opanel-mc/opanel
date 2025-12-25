package net.opanel.bukkit_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BaseBukkitOfflinePlayer implements OPanelPlayer {
    protected final JavaPlugin plugin;
    protected final TaskRunner runner;
    protected final OfflinePlayer player;
    protected final Server server;
    protected final Path playerDataPath;

    public BaseBukkitOfflinePlayer(JavaPlugin plugin, Server server, OfflinePlayer player) {
        this.plugin = plugin;
        runner = (TaskRunner) plugin;
        this.server = server;
        this.player = player;

        if(player.isOnline()) throw new IllegalStateException("The player is offline.");

        String uuid = player.getUniqueId().toString();
        playerDataPath = server.getWorlds().get(0).getWorldFolder().toPath().resolve("playerdata/"+ uuid +".dat");
        if(!Files.exists(playerDataPath)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }
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
        return false;
    }

    @Override
    public boolean isOp() {
        return player.isOp();
    }

    @Override
    public boolean isBanned() {
        return player.isBanned();
    }

    @Override
    public OPanelGameMode getGameMode() {
        try {
            ReadWriteNBT nbt = NBT.readFile(playerDataPath.toFile());
            int gamemode = nbt.getInteger("playerGameType");
            return OPanelGameMode.fromId(gamemode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        try {
            ReadWriteNBT nbt = NBT.readFile(playerDataPath.toFile());
            nbt.setInteger("playerGameType", gamemode.getId());
            NBT.writeFile(playerDataPath.toFile(), nbt);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public void kick(String reason) {
        throw new IllegalStateException("The player is offline.");
    }

    @Override
    public int getPing() {
        throw new IllegalStateException("The player is offline.");
    }
}
