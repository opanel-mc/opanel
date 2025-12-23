package net.opanel.folia_1_21;

import net.opanel.bukkit_helper.BaseBukkitOfflinePlayer;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.profile.PlayerProfile;

import java.util.Date;

public class FoliaOfflinePlayer extends BaseBukkitOfflinePlayer implements OPanelPlayer {
    private final PlayerProfile profile;

    public FoliaOfflinePlayer(Main plugin, Server server, OfflinePlayer player) {
        super(plugin, server, player);

        profile = player.getPlayerProfile();
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        runner.runTask(() -> player.ban(reason, (Date) null, null));
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        BanList<PlayerProfile> banList = server.getBanList(BanList.Type.PROFILE);
        BanEntry<PlayerProfile> banEntry = banList.getBanEntry(profile);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        BanList<PlayerProfile> banList = server.getBanList(BanList.Type.PROFILE);
        runner.runTask(() -> banList.pardon(profile));
    }
}