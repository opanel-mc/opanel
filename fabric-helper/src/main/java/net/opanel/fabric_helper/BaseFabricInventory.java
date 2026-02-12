package net.opanel.fabric_helper;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.opanel.common.OPanelInventory;

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
    public void setItems(List<OPanelItemStack> items) throws CommandSyntaxException {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        for(OPanelItemStack item : items) {
            inventory.setStack(item.slot, toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelItemStack item) throws CommandSyntaxException {
        player.getInventory().setStack(item.slot, toItemStack(item));
    }

    protected abstract ItemStack toItemStack(OPanelItemStack item) throws CommandSyntaxException;
}
