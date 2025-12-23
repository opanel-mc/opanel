package net.opanel.spigot_1_20;

import net.opanel.bukkit_helper.BaseBukkitOfflinePlayer;
import net.opanel.bukkit_helper.TaskRunner;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.profile.PlayerProfile;

public class SpigotOfflinePlayer extends BaseBukkitOfflinePlayer implements OPanelPlayer {
    private final PlayerProfile profile;

    public SpigotOfflinePlayer(Main plugin, Server server, OfflinePlayer player) {
        super(plugin, server, player);

        profile = player.getPlayerProfile();
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        runner.runTask(() -> plugin.getServer().getBanList(BanList.Type.NAME).addBan(player.getName(), reason, null, null));
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
        runner.runTask(() -> banList.pardon(player.getName()));
    }
}
