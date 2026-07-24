package net.opanel.folia_1_20_5;

import net.opanel.paper_helper.BasePaperOfflineInventory;

import java.nio.file.Path;

public class FoliaOfflineInventory extends BasePaperOfflineInventory {
    public FoliaOfflineInventory(Path playerDataPath) {
        super(playerDataPath);
    }

    @Override
    protected String keyOfCount() {
        return "count";
    }

    @Override
    protected String keyOfNBT() {
        return "components";
    }

    @Override
    protected boolean usesEquipmentTag() {
        return false;
    }
}
