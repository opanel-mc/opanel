package net.opanel.fabric_helper;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelInventory;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFabricInventory implements OPanelInventory {
    protected final ServerPlayerEntity player;

    public BaseFabricInventory(ServerPlayerEntity player) {
        this.player = player;
    }

    protected abstract String itemToId(Item item);
    protected abstract Item idToItem(String id);

    @Override
    public int getSize() {
        return player.getInventory().size();
    }

    @Override
    public List<OPanelItemStack> getItems() {
        PlayerInventory inventory = player.getInventory();
        int size = getSize();
        List<OPanelItemStack> items = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            ItemStack stack = inventory.getStack(i);
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
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        for(OPanelItemStack item : items) {
            inventory.setStack(item.slot, toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        player.getInventory().setStack(item.slot, toItemStack(item));
    }

    protected ItemStack toItemStack(OPanelItemStack item) {
        if(item == null || item.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(idToItem(item.id), Math.max(1, item.count));
    }
}
