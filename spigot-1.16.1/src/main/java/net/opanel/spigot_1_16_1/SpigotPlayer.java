package net.opanel.spigot_1_16_1;

import net.opanel.bukkit_helper.BaseBukkitPlayer;
import net.opanel.bukkit_helper.TaskRunner;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

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
        try {
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Field pingField = craftPlayer.getClass().getDeclaredField("ping");
            pingField.setAccessible(true); // to prevent private flag
            return pingField.getInt(craftPlayer);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
