package net.opanel.paper_1_21;

import net.opanel.paper_helper.BasePaperInventory;
import net.opanel.paper_helper.TaskRunner;
import org.bukkit.entity.Player;

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