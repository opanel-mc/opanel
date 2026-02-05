package net.opanel.forge_helper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.opanel.common.OPanelInventory;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseForgeInventory implements OPanelInventory {
    protected final ServerPlayer player;

    public BaseForgeInventory(ServerPlayer player) {
        this.player = player;
    }

    protected abstract String itemToId(Item item);
    protected abstract Item idToItem(String id);

    @Override
    public int getSize() {
        return player.getInventory().getContainerSize();
    }

    @Override
    public List<OPanelItemStack> getItems() {
        Inventory inventory = player.getInventory();
        int size = getSize();
        List<OPanelItemStack> items = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if(stack.isEmpty()) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
                continue;
            }

            final String id = itemToId(stack.getItem());
            items.add(new OPanelItemStack(i, id, stack.getCount(), null));
        }
        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) {
        Inventory inventory = player.getInventory();
        inventory.clearContent();

        for(OPanelItemStack item : items) {
            inventory.setItem(item.slot, toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        player.getInventory().setItem(item.slot, toItemStack(item));
    }

    protected ItemStack toItemStack(OPanelItemStack item) {
        if(item == null || item.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(idToItem(item.id), Math.max(1, item.count));
    }
}
