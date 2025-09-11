package net.opanel.spigot_1_20;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.profile.PlayerProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class SpigotOfflinePlayer implements OPanelPlayer {
    private final Main plugin;
    private final OfflinePlayer player;
    private final Server server;
    private final Path playerDataPath;
    private final PlayerProfile profile;

    public SpigotOfflinePlayer(Main plugin, Server server, OfflinePlayer player) {
        this.plugin = plugin;
        this.server = server;
        this.player = player;

        if(player.isOnline()) throw new IllegalStateException("The player is offline.");

        String uuid = player.getUniqueId().toString();
        playerDataPath = server.getWorlds().getFirst().getWorldFolder().toPath().resolve("playerdata/"+ uuid +".dat");
        if(!Files.exists(playerDataPath)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }

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
            switch(gamemode) {
                case 2 -> { return OPanelGameMode.ADVENTURE; }
                case 0 -> { return OPanelGameMode.SURVIVAL; }
                case 1 -> { return OPanelGameMode.CREATIVE; }
                case 3 -> { return OPanelGameMode.SPECTATOR; }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        try {
            ReadWriteNBT nbt = NBT.readFile(playerDataPath.toFile());
            switch(gamemode) {
                case ADVENTURE -> nbt.setInteger("playerGameType", 2);
                case SURVIVAL -> nbt.setInteger("playerGameType", 0);
                case CREATIVE -> nbt.setInteger("playerGameType", 1);
                case SPECTATOR -> nbt.setInteger("playerGameType", 3);
            }
            NBT.writeFile(playerDataPath.toFile(), nbt);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        throw new IllegalStateException("The player is offline.");
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        plugin.runTask(() -> plugin.getServer().getBanList(BanList.Type.NAME).addBan(player.getName(), reason, null, null));
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        BanList banList = server.getBanList(BanList.Type.NAME);
        BanEntry banEntry = banList.getBanEntry(player.getName());
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        BanList banList = server.getBanList(BanList.Type.NAME);
        plugin.runTask(() -> banList.pardon(player.getName()));
    }

    @Override
    public int getPing() {
        throw new IllegalStateException("The player is offline.");
    }
}
