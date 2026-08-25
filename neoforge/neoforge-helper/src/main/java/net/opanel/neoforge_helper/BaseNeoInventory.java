package net.opanel.neoforge_helper;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;

import java.util.List;

public abstract class BaseNeoInventory implements OPanelInventory {
    protected final ServerPlayer player;

    public BaseNeoInventory(ServerPlayer player) {
        this.player = player;
    }

    protected String itemToId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    protected ItemStack getItemStack(OPanelInventoryType inventoryType, int slot) {
        if(inventoryType == OPanelInventoryType.ENDER_CHEST) {
            return player.getEnderChestInventory().getItem(slot);
        }
        return player.getInventory().getItem(inventoryType == OPanelInventoryType.EQUIPMENTS ? equipmentSlot(slot) : slot);
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
            player.getEnderChestInventory().setItem(slot, item);
            return;
        }
        player.getInventory().setItem(inventoryType == OPanelInventoryType.EQUIPMENTS ? equipmentSlot(slot) : slot, item);
    }

    private int equipmentSlot(int slot) {
        return slot == 4 ? 40 : 39 - slot;
    }

    protected abstract ItemStack toItemStack(OPanelItemStack item) throws CommandSyntaxException;
}
