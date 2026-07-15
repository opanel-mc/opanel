package net.opanel.paper_1_16_1;

import net.opanel.paper_helper.BasePaperPlayer;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PaperPlayer extends BasePaperPlayer implements OPanelPlayer {
    public PaperPlayer(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void kick(String reason) {
        runner.runTask(() -> player.kickPlayer(reason));
    }

    @Override
    public PaperInventory getInventory() {
        return new PaperInventory(runner, player);
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
