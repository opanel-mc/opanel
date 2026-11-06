package net.opanel.paper_1_21_9;

import net.kyori.adventure.text.Component;
import net.opanel.paper_helper.BasePaperPlayer;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.Map;

import java.util.Date;

public class PaperPlayer extends BasePaperPlayer implements OPanelPlayer {
    public PaperPlayer(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public Map<String, Object> serializePosition() {
        return serializePosition(player.getLocation());
    }

    static Map<String, Object> serializePosition(Location location) {
        return Map.of(
                "x", location.getX(),
                "y", location.getY(),
                "z", location.getZ(),
                "save", location.getWorld().getWorldFolder().getName()
        );
    }

    @Override
    public void kick(String reason) {
        runner.runTask(() -> player.kick(Component.text(reason)));
    }

    @Override
    public PaperInventory getInventory() {
        return new PaperInventory(runner, player);
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        runner.runTask(() -> player.ban(reason, (Date) null, null, true));
    }

    @Override
    public int getPing() {
        return player.getPing();
    }
}
