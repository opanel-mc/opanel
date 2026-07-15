package net.opanel.paper_1_21_9;

import net.opanel.paper_helper.BasePaperInventory;
import net.opanel.paper_helper.TaskRunner;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PaperInventory extends BasePaperInventory {
    public PaperInventory(TaskRunner runner, Player player) {
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
