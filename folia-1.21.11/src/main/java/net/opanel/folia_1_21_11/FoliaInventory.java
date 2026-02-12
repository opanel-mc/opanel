package net.opanel.folia_1_21_11;

import net.opanel.bukkit_helper.BaseBukkitInventory;
import net.opanel.bukkit_helper.TaskRunner;
import org.bukkit.entity.Player;

public class FoliaInventory extends BaseBukkitInventory {
    public FoliaInventory(TaskRunner runner, Player player) {
        super(runner, player);
    }

    @Override
    protected String keyOfCount() {
        return "count";
    }

    @Override
    protected String keyOfNBT() {
        return "components";
    }
}