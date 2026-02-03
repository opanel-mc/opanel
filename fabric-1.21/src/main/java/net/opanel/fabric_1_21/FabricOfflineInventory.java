package net.opanel.fabric_1_21;

import net.opanel.fabric_helper.BaseFabricOfflineInventory;

import java.nio.file.Path;

public class FabricOfflineInventory extends BaseFabricOfflineInventory {
    public FabricOfflineInventory(Path playerDataPath) {
        super(playerDataPath);
    }
}