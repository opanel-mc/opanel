package net.opanel.spigot_1_19_4;

import net.opanel.bukkit_helper.BaseBukkitInventory;
import net.opanel.bukkit_helper.TaskRunner;
import org.bukkit.entity.Player;

public class SpigotInventory extends BaseBukkitInventory {
    public SpigotInventory(TaskRunner runner, Player player) {
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