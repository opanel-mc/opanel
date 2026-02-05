package net.opanel.fabric_helper;

import net.opanel.common.OPanelInventory;

import java.io.IOException;
import java.nio.file.Path;

public abstract class BaseFabricOfflineInventory implements OPanelInventory {
    protected final Path playerDataPath;

    public BaseFabricOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;
    }

    protected abstract void saveNbt() throws IOException;
}