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

public class BaseBukkitOfflineInventory implements OPanelInventory {
    protected final Path playerDataPath;
    protected ReadWriteNBT nbt;

    public BaseBukkitOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;
        try {
            nbt = NBT.readFile(playerDataPath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            int count = itemNbt.getByte("count");
            items.add(new OPanelItemStack(slot, id, count, null));
            nextSlot = slot + 1;
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
                itemNbt.setByte("count", (byte) item.count);
                list.addCompound(itemNbt);
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        if(item.isEmpty()) return;

        try {
            ReadWriteNBTCompoundList list = nbt.getCompoundList("Inventory");
            if(list == null) return;

            for(int i = list.size() - 1; i >= 0; i--) {
                ReadWriteNBT itemNbt = list.get(i);
                if(itemNbt.getByte("Slot") == (byte) item.slot) {
                    list.remove(i);
                }
            }

            ReadWriteNBT itemNbt = NBT.createNBTObject();
            itemNbt.setByte("Slot", (byte) item.slot);
            itemNbt.setString("id", item.id);
            itemNbt.setByte("count", (byte) item.count);
            list.addCompound(itemNbt);
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}