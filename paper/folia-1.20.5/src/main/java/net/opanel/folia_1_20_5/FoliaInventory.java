package net.opanel.folia_1_20_5;

import net.opanel.paper_helper.BasePaperInventory;
import net.opanel.paper_helper.TaskRunner;
import org.bukkit.entity.Player;

public class FoliaInventory extends BasePaperInventory {
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