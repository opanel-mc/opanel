package net.opanel.spigot_1_20;

import net.opanel.bukkit_helper.BaseBukkitPlayer;
import net.opanel.bukkit_helper.TaskRunner;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class SpigotPlayer extends BaseBukkitPlayer implements OPanelPlayer {
    public SpigotPlayer(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void kick(String reason) {
        runner.runTask(() -> player.kickPlayer(reason));
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        runner.runTask(() -> {
            player.getServer().getBanList(BanList.Type.NAME).addBan(player.getName(), reason, null, null);
            player.kickPlayer(reason);
        });
    }

    @Override
    public int getPing() {
        return player.getPing();
    }
}
