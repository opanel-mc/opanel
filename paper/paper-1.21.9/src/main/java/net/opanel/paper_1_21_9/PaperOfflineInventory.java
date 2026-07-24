package net.opanel.paper_1_21_9;

import net.opanel.paper_helper.BasePaperOfflineInventory;

import java.nio.file.Path;

public class PaperOfflineInventory extends BasePaperOfflineInventory {
    public PaperOfflineInventory(Path playerDataPath) {
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
        return true;
    }
}
