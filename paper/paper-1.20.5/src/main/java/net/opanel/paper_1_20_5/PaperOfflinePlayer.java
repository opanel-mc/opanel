package net.opanel.paper_1_20_5;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import net.opanel.paper_helper.BasePaperOfflinePlayer;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.ban.ProfileBanList;

import java.util.Date;

public class PaperOfflinePlayer extends BasePaperOfflinePlayer implements OPanelPlayer {
    private final PlayerProfile profile;

    public PaperOfflinePlayer(Main plugin, Server server, OfflinePlayer player) {
        super(plugin, server, player);

        profile = player.getPlayerProfile();
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        runner.runTask(() -> plugin.getServer().getBanList(BanListType.PROFILE).addBan(profile, reason, (Date) null, null));
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        ProfileBanList banList = server.getBanList(BanListType.PROFILE);
        BanEntry<PlayerProfile> banEntry = banList.getBanEntry(profile);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        ProfileBanList banList = server.getBanList(BanListType.PROFILE);
        runner.runTask(() -> banList.pardon(profile));
    }

    @Override
    public PaperOfflineInventory getInventory() {
        return new PaperOfflineInventory(playerDataPath);
    }
}
