package net.opanel.bukkit_1_21_5;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import net.kyori.adventure.text.Component;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.BanEntry;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Date;

public class BukkitPlayer implements OPanelPlayer {
    private final Player player;
    private final Server server;
    private final PlayerProfile profile;

    public BukkitPlayer(Player player) {
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
        return player.isOnline();
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
        switch(gamemode) {
            case ADVENTURE -> player.setGameMode(GameMode.ADVENTURE);
            case SURVIVAL -> player.setGameMode(GameMode.SURVIVAL);
            case CREATIVE -> player.setGameMode(GameMode.CREATIVE);
            case SPECTATOR -> player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @Override
    public void giveOp() {
        if(isOp()) return;
        player.setOp(true);
    }

    @Override
    public void depriveOp() {
        if(!isOp()) return;
        player.setOp(false);
    }

    @Override
    public void kick(String reason) {
        if(!isOnline()) return;
        if (reason == null) {
            player.kick();
        } else {
            player.kick(Component.text(reason));
        }
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        player.ban(reason, (Date) null, null, true);
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        BanEntry<PlayerProfile> banEntry = server.getBanList(BanListType.PROFILE).getBanEntry(profile);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        server.getBanList(BanListType.PROFILE).pardon(profile);
    }

    @Override
    public int getPing() {
        if(!isOnline()) {
            throw new IllegalStateException("The player is offline.");
        }
        return player.getPing();
    }
}
