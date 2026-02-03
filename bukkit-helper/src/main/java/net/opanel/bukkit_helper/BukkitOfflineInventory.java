package net.opanel.bukkit_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTList;
import net.opanel.common.OPanelInventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BukkitOfflineInventory implements OPanelInventory {
    private final Path playerDataPath;

    public BukkitOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;
    }

    @Override
    public int getSize() {
        return 41;
    }

    @Override
    public List<OPanelItemStack> getItems() {
        List<OPanelItemStack> items = new ArrayList<>();
        try {
            ReadWriteNBT nbt = NBT.readFile(playerDataPath.toFile());
            ReadWriteNBTList<ReadWriteNBT> list = nbt.getCompoundList("Inventory");
            if(list == null) return items;

            for(ReadWriteNBT itemNbt : list) {
                int slot = itemNbt.getByte("Slot");
                String id = itemNbt.getString("id");
                int count = itemNbt.getByte("Count");
                items.add(new OPanelItemStack(slot, id, count, null));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) {
        try {
            ReadWriteNBT nbt = NBT.readFile(playerDataPath.toFile());
            ReadWriteNBTList<ReadWriteNBT> list = nbt.getCompoundList("Inventory");
            if(list != null) list.clear();

            if(items != null) {
                for(OPanelItemStack item : items) {
                    if(item == null || item.isEmpty()) continue;
                    ReadWriteNBT itemNbt = list.addCompound();
                    itemNbt.setByte("Slot", (byte) item.slot);
                    itemNbt.setString("id", item.id);
                    itemNbt.setByte("Count", (byte) item.count);
                }
            }

            NBT.writeFile(playerDataPath.toFile(), nbt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        if(item == null) return;
        try {
            ReadWriteNBT nbt = NBT.readFile(playerDataPath.toFile());
            ReadWriteNBTList<ReadWriteNBT> list = nbt.getCompoundList("Inventory");
            if(list == null) return;

            for(int i = list.size() - 1; i >= 0; i--) {
                ReadWriteNBT itemNbt = list.get(i);
                if(itemNbt.getByte("Slot") == (byte) item.slot) {
                    list.remove(i);
                }
            }

            if(!item.isEmpty()) {
                ReadWriteNBT itemNbt = list.addCompound();
                itemNbt.setByte("Slot", (byte) item.slot);
                itemNbt.setString("id", item.id);
                itemNbt.setByte("Count", (byte) item.count);
            }

            NBT.writeFile(playerDataPath.toFile(), nbt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}