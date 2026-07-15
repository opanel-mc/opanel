package net.opanel.folia_26_1_2;

import net.kyori.adventure.text.Component;
import net.opanel.annotation.Rewrite;
import net.opanel.paper_helper.BasePaperPlayer;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.Date;

public class FoliaPlayer extends BasePaperPlayer implements OPanelPlayer {
    public FoliaPlayer(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public FoliaInventory getInventory() {
        return new FoliaInventory(runner, player);
    }

    @Rewrite
    @Override
    public void setGameMode(OPanelGameMode gamemode) {
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> {
            switch(gamemode) {
                case ADVENTURE -> player.setGameMode(GameMode.ADVENTURE);
                case SURVIVAL -> player.setGameMode(GameMode.SURVIVAL);
                case CREATIVE -> player.setGameMode(GameMode.CREATIVE);
                case SPECTATOR -> player.setGameMode(GameMode.SPECTATOR);
            }
        }, null);
    }

    @Rewrite
    @Override
    public void giveOp() {
        if(isOp()) return;
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> player.setOp(true), null);
    }

    @Rewrite
    @Override
    public void depriveOp() {
        if(!isOp()) return;
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> player.setOp(false), null);
    }

    @Override
    public void kick(String reason) {
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> player.kick(Component.text(reason)), null);
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        // Use entity scheduler for player-specific tasks in Folia
        player.getScheduler().run(plugin, (task) -> player.ban(reason, (Date) null, null, true), null);
    }

    @Override
    public int getPing() {
        return player.getPing();
    }
}