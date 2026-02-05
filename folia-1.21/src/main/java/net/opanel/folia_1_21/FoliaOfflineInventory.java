package net.opanel.folia_1_21;

import net.opanel.bukkit_helper.BaseBukkitOfflineInventory;

import java.nio.file.Path;

public class FoliaOfflineInventory extends BaseBukkitOfflineInventory {
    public FoliaOfflineInventory(Path playerDataPath) {
        super(playerDataPath);
    }

    @Override
    protected String keyOfCount() {
        return "count";
    }
}
