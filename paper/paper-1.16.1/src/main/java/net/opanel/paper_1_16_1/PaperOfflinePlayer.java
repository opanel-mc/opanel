package net.opanel.paper_1_16_1;

import net.opanel.paper_helper.BasePaperOfflinePlayer;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;

public class PaperOfflinePlayer extends BasePaperOfflinePlayer implements OPanelPlayer {
    public PaperOfflinePlayer(Main plugin, Server server, OfflinePlayer player) {
        super(plugin, server, player);
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

    @Override
    public PaperOfflineInventory getInventory() {
        return new PaperOfflineInventory(playerDataPath);
    }
}
