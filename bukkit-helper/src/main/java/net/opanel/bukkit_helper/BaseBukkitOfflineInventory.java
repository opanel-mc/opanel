package net.opanel.bukkit_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTList;
import net.opanel.common.OPanelInventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseBukkitOfflineInventory implements OPanelInventory {
    protected final Path playerDataPath;
    protected ReadWriteNBT nbt;

    private final String KEY_OF_COUNT = keyOfCount();

    public BaseBukkitOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;
        try {
            nbt = NBT.readFile(playerDataPath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * In MC versions < 1.20.5, the nbt key that represents the item amount is "Count",
     * while in MC versions >= 1.20.5, that key is changed to "count".
     */
    protected abstract String keyOfCount();

    protected void saveNbt() throws IOException {
        NBT.writeFile(playerDataPath.toFile(), nbt);
    }

    @Override
    public int getSize() {
        return nbt.getCompoundList("Inventory").size();
    }

    @Override
    public List<OPanelItemStack> getItems() {
        List<OPanelItemStack> items = new ArrayList<>();
        ReadWriteNBTCompoundList list = nbt.getCompoundList("Inventory");
        if(list == null) return items;

        int nextSlot = 0;
        for(ReadWriteNBT itemNbt : list) {
            int slot = itemNbt.getByte("Slot");
            if(slot > nextSlot) {
                for(int i = nextSlot; i < slot; i++) {
                    items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
                }
            }

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte(KEY_OF_COUNT);
            items.add(new OPanelItemStack(slot, id, count, null));
            nextSlot = slot + 1;
        }

        if(nextSlot <= 35) {
            for(int i = nextSlot; i < 36; i++) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
            }
        }

        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) {
        try {
            ReadWriteNBTCompoundList list = nbt.getCompoundList("Inventory");
            if(list == null) return;
            list.clear();

            for(OPanelItemStack item : items) {
                if(item == null || item.isEmpty()) continue;
                ReadWriteNBT itemNbt = NBT.createNBTObject();
                itemNbt.setByte("Slot", (byte) item.slot);
                itemNbt.setString("id", item.id);
                itemNbt.setByte(KEY_OF_COUNT, (byte) item.count);
                list.addCompound(itemNbt);
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        try {
            ReadWriteNBTCompoundList list = nbt.getCompoundList("Inventory");
            if(list == null) return;

            // Insert to the last
            if(item.slot > list.get(list.size() - 1).getByte("Slot")) {
                ReadWriteNBT newItemNbt = NBT.createNBTObject();
                newItemNbt.setByte("Slot", (byte) item.slot);
                newItemNbt.setString("id", item.id);
                newItemNbt.setByte(KEY_OF_COUNT, (byte) item.count);
                list.addCompound(newItemNbt);
                saveNbt();
                return;
            }

            for(int i = 0; i < list.size(); i++) {
                ReadWriteNBT itemNbt = list.get(i);
                int slot = itemNbt.getByte("Slot");

                // Insert into an empty slot
                if(slot > item.slot) {
                    ReadWriteNBT newItemNbt = NBT.createNBTObject();
                    newItemNbt.setByte("Slot", (byte) item.slot);
                    newItemNbt.setString("id", item.id);
                    newItemNbt.setByte(KEY_OF_COUNT, (byte) item.count);
                    BukkitUtils.addCompoundToNBTList(list, newItemNbt, i);
                    break;
                }
                // Remove the item
                if(slot == item.slot && item.isEmpty()) {
                    list.remove(i);
                    break;
                }
                // Update the slot item
                if(slot == item.slot) {
                    itemNbt.setString("id", item.id);
                    itemNbt.setByte(KEY_OF_COUNT, (byte) item.count);
                    break;
                }
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}