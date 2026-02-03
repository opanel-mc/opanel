package net.opanel.forge_helper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.opanel.common.OPanelInventory;

import java.util.ArrayList;
import java.util.List;

public class ForgeInventory implements OPanelInventory {
    private final ServerPlayer player;

    public ForgeInventory(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public int getSize() {
        return player.getInventory().getContainerSize();
    }

    @Override
    public List<OPanelItemStack> getItems() {
        int size = getSize();
        List<OPanelItemStack> items = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if(stack == null || stack.isEmpty()) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
            } else {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                items.add(new OPanelItemStack(i, id, stack.getCount(), null));
            }
        }
        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) {
        player.getInventory().clearContent();
        if(items == null) return;

        for(OPanelItemStack item : items) {
            player.getInventory().setItem(item.slot, toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        if(item == null) return;
        player.getInventory().setItem(item.slot, toItemStack(item));
    }

    private ItemStack toItemStack(OPanelItemStack item) {
        if(item == null || item.isEmpty()) return ItemStack.EMPTY;
        Item mcItem = BuiltInRegistries.ITEM.get(new ResourceLocation(item.id));
        return new ItemStack(mcItem, Math.max(1, item.count));
    }
}