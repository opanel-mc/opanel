package net.opanel.neoforge_1_21_1;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.opanel.common.OPanelInventory;

import java.util.ArrayList;
import java.util.List;

public class NeoInventory implements OPanelInventory {
    protected final ServerPlayer player;

    public NeoInventory(ServerPlayer player) {
        this.player = player;
    }

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

            final String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
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
        Item mcItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(item.id));
        return new ItemStack(mcItem, Math.max(1, item.count));
    }
}
