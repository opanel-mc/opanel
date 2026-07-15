package net.opanel.paper_26_1;

import net.kyori.adventure.text.Component;
import net.opanel.paper_helper.BasePaperPlayer;
import net.opanel.common.OPanelPlayer;
import org.bukkit.entity.Player;

import java.util.Date;

public class PaperPlayer extends BasePaperPlayer implements OPanelPlayer {
    public PaperPlayer(Main plugin, Player player) {
        super(plugin, player);
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
