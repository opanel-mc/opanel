package net.opanel.folia_1_20;

import net.opanel.paper_helper.BasePaperInventory;
import net.opanel.paper_helper.TaskRunner;
import org.bukkit.entity.Player;

public class FoliaInventory extends BasePaperInventory {
    public FoliaInventory(TaskRunner runner, Player player) {
        super(runner, player);
    }

    @Override
    protected String keyOfCount() {
        return "Count";
    }

    @Override
    protected String keyOfNBT() {
        return "tag";
    }
}
