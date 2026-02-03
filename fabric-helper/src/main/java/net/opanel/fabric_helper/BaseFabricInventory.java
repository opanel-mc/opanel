package net.opanel.fabric_helper;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.opanel.common.OPanelInventory;

import java.util.ArrayList;
import java.util.List;

public class BaseFabricInventory implements OPanelInventory {
    protected final ServerPlayerEntity player;

    public BaseFabricInventory(ServerPlayerEntity player) {
        this.player = player;
    }

    @Override
    public int getSize() {
        return player.getInventory().size();
    }

    @Override
    public List<OPanelItemStack> getItems() {
        int size = getSize();
        List<OPanelItemStack> items = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if(stack == null || stack.isEmpty()) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
            } else {
                String id = Registries.ITEM.getId(stack.getItem()).toString();
                items.add(new OPanelItemStack(i, id, stack.getCount(), null));
            }
        }
        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) {
        player.getInventory().clear();
        if(items == null) return;

        for(OPanelItemStack item : items) {
            player.getInventory().setStack(item.slot, toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        if(item == null) return;
        player.getInventory().setStack(item.slot, toItemStack(item));
    }

    protected ItemStack toItemStack(OPanelItemStack item) {
        if(item == null || item.isEmpty()) return ItemStack.EMPTY;
        Item mcItem = Registries.ITEM.get(new Identifier(item.id));
        return new ItemStack(mcItem, Math.max(1, item.count));
    }
}