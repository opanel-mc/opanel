package net.opanel.paper_1_19_4;

import net.opanel.paper_helper.BasePaperOfflineInventory;

import java.nio.file.Path;

public class PaperOfflineInventory extends BasePaperOfflineInventory {
    public PaperOfflineInventory(Path playerDataPath) {
        super(playerDataPath);
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
