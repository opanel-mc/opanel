package net.opanel.folia_1_21;

import net.opanel.bukkit_helper.BaseBukkitInventory;
import net.opanel.bukkit_helper.TaskRunner;
import org.bukkit.entity.Player;

public class FoliaInventory extends BaseBukkitInventory {
    public FoliaInventory(TaskRunner runner, Player player) {
        super(runner, player);
    }
}