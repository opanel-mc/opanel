package net.opanel.forge_helper;

import net.opanel.common.OPanelInventory;

import java.io.IOException;
import java.nio.file.Path;

public abstract class BaseForgeOfflineInventory implements OPanelInventory {
    protected final Path playerDataPath;

    public BaseForgeOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;
    }

    protected abstract void saveNbt() throws IOException;
}
