package net.opanel.bukkit_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
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

    private final String KEY_OF_COUNT = keyOfCount();
    private final String KEY_OF_NBT = keyOfNBT();

    public BaseBukkitInventory(TaskRunner runner, Player player) {
        this.runner = runner;
        this.player = player;
    }

    protected abstract String keyOfCount();
    protected abstract String keyOfNBT();

    @Override
    public int getSize() {
        return player.getInventory().getSize();
    }

    @Override
    public int getEnderSize() {
        return player.getEnderChest().getSize();
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
    public List<OPanelItemStack> getEnderItems() {
        Inventory inventory = player.getEnderChest();
        int size = getEnderSize();
        List<OPanelItemStack> items = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if(stack == null || stack.getType() == Material.AIR) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null, "ender"));
                continue;
            }

            ReadWriteNBT components = NBT.itemStackToNBT(stack).getCompound(KEY_OF_NBT);
            items.add(new OPanelItemStack(
                    i,
                    stack.getType().getKey().toString(),
                    stack.getAmount(),
                    components == null ? null : components.toString(),
                    "ender"
            ));
        }
        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) {
        runner.runTask(() -> {
            Inventory inventory = player.getInventory();
            inventory.clear();

            for(OPanelItemStack item : items) {
                try {
                    inventory.setItem(item.slot, toItemStack(item));
                } catch (NbtApiException e) {
                    //
                }
            }
        });
    }

    @Override
    public void setItem(OPanelItemStack item) throws NbtApiException {
        runner.runTask(() -> {
            try {
                player.getInventory().setItem(item.slot, toItemStack(item));
            } catch (NbtApiException e) {
                //
            }
        });
    }

    @Override
    public void setEnderItem(OPanelItemStack item) throws NbtApiException {
        runner.runTask(() -> {
            try {
                player.getEnderChest().setItem(item.slot, toItemStack(item));
            } catch (NbtApiException e) {
                //
            }
        });
    }

    @Override
    public boolean canReadEnderChest() {
        return true;
    }

    @Override
    public boolean canWriteEnderChest() {
        return true;
    }

    protected ItemStack toItemStack(OPanelItemStack item) throws NbtApiException {
        if(item == null || item.isEmpty()) return null;
        Material material = Material.matchMaterial(item.id);
        if(material == null || material == Material.AIR) return null;

        ReadWriteNBT itemNbt = NBT.createNBTObject();
        itemNbt.setByte("Slot", (byte) item.slot);
        itemNbt.setString("id", item.id);
        itemNbt.setByte(KEY_OF_COUNT, (byte) Math.max(1, item.count));
        if(item.snbt != null) {
            itemNbt.set(KEY_OF_NBT, NBT.parseNBT(item.snbt), NBTHandlers.STORE_READWRITE_TAG);
        }
        return NBT.itemStackFromNBT(itemNbt);
    }
}
