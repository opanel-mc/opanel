package net.opanel.paper_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class BasePaperInventory implements OPanelInventory {
    protected final TaskRunner runner;
    protected final Player player;

    private final String KEY_OF_COUNT = keyOfCount();
    private final String KEY_OF_NBT = keyOfNBT();

    public BasePaperInventory(TaskRunner runner, Player player) {
        this.runner = runner;
        this.player = player;
    }

    protected abstract String keyOfCount();
    protected abstract String keyOfNBT();

    @Override
    public List<OPanelItemStack> getItems(OPanelInventoryType inventoryType) {
        Inventory inventory = getInventory(inventoryType);
        int size = getSize(inventoryType);
        List<OPanelItemStack> items = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            ItemStack stack = getItemStack(inventoryType, inventory, i);
            if(stack == null || stack.getType() == Material.AIR) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
                continue;
            }

            ReadWriteNBT components = NBT.itemStackToNBT(stack).getCompound(KEY_OF_NBT);
            items.add(new OPanelItemStack(
                i,
                stack.getType().getKey().toString(),
                stack.getAmount(),
                components == null ? null : components.toString()
            ));
        }
        return items;
    }

    @Override
    public void setItems(OPanelInventoryType inventoryType, List<OPanelItemStack> items) {
        runner.runTask(() -> {
            Inventory inventory = getInventory(inventoryType);
            for(int slot = 0; slot < getSize(inventoryType); slot++) {
                setItemStack(inventoryType, inventory, slot, null);
            }

            for(OPanelItemStack item : items) {
                if(item == null || item.slot() < 0 || item.slot() >= getSize(inventoryType)) continue;
                try {
                    setItemStack(inventoryType, inventory, item.slot(), toItemStack(item));
                } catch (NbtApiException e) {
                    //
                }
            }
        });
    }

    @Override
    public void setItem(OPanelInventoryType inventoryType, OPanelItemStack item) throws NbtApiException {
        runner.runTask(() -> {
            try {
                Inventory inventory = getInventory(inventoryType);
                setItemStack(inventoryType, inventory, item.slot(), toItemStack(item));
            } catch (NbtApiException e) {
                //
            }
        });
    }

    private Inventory getInventory(OPanelInventoryType inventoryType) {
        return inventoryType == OPanelInventoryType.ENDER_CHEST
            ? player.getEnderChest()
            : player.getInventory();
    }

    private ItemStack getItemStack(OPanelInventoryType inventoryType, Inventory inventory, int slot) {
        if(inventoryType != OPanelInventoryType.EQUIPMENTS) return inventory.getItem(slot);
        return inventory.getItem(equipmentSlot(slot));
    }

    private void setItemStack(OPanelInventoryType inventoryType, Inventory inventory, int slot, ItemStack item) {
        inventory.setItem(inventoryType == OPanelInventoryType.EQUIPMENTS ? equipmentSlot(slot) : slot, item);
    }

    private int equipmentSlot(int slot) {
        return slot == 4 ? 40 : 39 - slot;
    }

    protected ItemStack toItemStack(OPanelItemStack item) throws NbtApiException {
        if(item == null || item.isEmpty()) return null;
        Material material = Material.matchMaterial(item.id());
        if(material == null || material == Material.AIR) return null;

        ReadWriteNBT itemNbt = NBT.createNBTObject();
        itemNbt.setByte("Slot", (byte) item.slot());
        itemNbt.setString("id", item.id());
        itemNbt.setByte(KEY_OF_COUNT, (byte) Math.max(1, item.count()));
        if(item.snbt() != null) {
            itemNbt.set(KEY_OF_NBT, NBT.parseNBT(item.snbt()), NBTHandlers.STORE_READWRITE_TAG);
        }
        return NBT.itemStackFromNBT(itemNbt);
    }
}
