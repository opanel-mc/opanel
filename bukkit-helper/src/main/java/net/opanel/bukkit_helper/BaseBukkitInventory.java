package net.opanel.bukkit_helper;

import net.opanel.common.OPanelInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseBukkitInventory implements OPanelInventory {
    protected final TaskRunner runner;
    protected final Player player;

    public BaseBukkitInventory(TaskRunner runner, Player player) {
        this.runner = runner;
        this.player = player;
    }

    @Override
    public int getSize() {
        return player.getInventory().getSize();
    }

    @Override
    public List<OPanelItemStack> getItems() {
        Inventory inventory = player.getInventory();
        int size = getSize();
        List<OPanelItemStack> items = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if(stack == null || stack.getType() == Material.AIR) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
                continue;
            }
            items.add(new OPanelItemStack(i, stack.getType().getKey().toString(), stack.getAmount(), null));
        }
        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) {
        runner.runTask(() -> {
            Inventory inventory = player.getInventory();
            inventory.clear();

            for(OPanelItemStack item : items) {
                inventory.setItem(item.slot, toItemStack(item));
            }
        });
    }

    @Override
    public void setItem(OPanelItemStack item) {
        runner.runTask(() -> player.getInventory().setItem(item.slot, toItemStack(item)));
    }

    protected ItemStack toItemStack(OPanelItemStack item) {
        if(item == null || item.isEmpty()) return null;
        Material material = Material.matchMaterial(item.id);
        if(material == null || material == Material.AIR) return null;
        return new ItemStack(material, Math.max(1, item.count));
    }
}