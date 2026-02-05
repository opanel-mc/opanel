package net.opanel.spigot_1_21;

import net.opanel.bukkit_helper.BaseBukkitOfflineInventory;

import java.nio.file.Path;

public class SpigotOfflineInventory extends BaseBukkitOfflineInventory {
    public SpigotOfflineInventory(Path playerDataPath) {
        super(playerDataPath);
    }

    @Override
    protected String keyOfCount() {
        return "count";
    }
}
