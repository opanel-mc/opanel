package net.opanel.fabric_helper;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;

import java.util.List;

public abstract class BaseFabricInventory implements OPanelInventory {
    protected final ServerPlayerEntity player;

    public BaseFabricInventory(ServerPlayerEntity player) {
        this.player = player;
    }

    protected abstract String itemToId(Item item);
    protected abstract Item idToItem(String id);

    protected ItemStack getItemStack(OPanelInventoryType inventoryType, int slot) {
        if(inventoryType == OPanelInventoryType.ENDER_CHEST) {
            return player.getEnderChestInventory().getStack(slot);
        }
        return player.getInventory().getStack(inventoryType == OPanelInventoryType.EQUIPMENTS ? equipmentSlot(slot) : slot);
    }

    @Override
    public void setItems(OPanelInventoryType inventoryType, List<OPanelItemStack> items) throws CommandSyntaxException {
        for(int slot = 0; slot < getSize(inventoryType); slot++) {
            setItemStack(inventoryType, slot, ItemStack.EMPTY);
        }

        for(OPanelItemStack item : items) {
            if(item == null || item.slot() < 0 || item.slot() >= getSize(inventoryType)) continue;
            setItemStack(inventoryType, item.slot(), toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelInventoryType inventoryType, OPanelItemStack item) throws CommandSyntaxException {
        setItemStack(inventoryType, item.slot(), toItemStack(item));
    }

    private void setItemStack(OPanelInventoryType inventoryType, int slot, ItemStack item) {
        if(inventoryType == OPanelInventoryType.ENDER_CHEST) {
            player.getEnderChestInventory().setStack(slot, item);
            return;
        }
        player.getInventory().setStack(inventoryType == OPanelInventoryType.EQUIPMENTS ? equipmentSlot(slot) : slot, item);
    }

    private int equipmentSlot(int slot) {
        return slot == 4 ? 40 : 39 - slot;
    }

    protected abstract ItemStack toItemStack(OPanelItemStack item) throws CommandSyntaxException;
}
