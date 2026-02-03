package net.opanel.fabric_helper;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.opanel.common.OPanelInventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BaseFabricOfflineInventory implements OPanelInventory {
    protected final Path playerDataPath;

    public BaseFabricOfflineInventory(Path playerDataPath) {
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
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            NbtList list = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
            for(int i = 0; i < list.size(); i++) {
                NbtCompound itemTag = list.getCompound(i);
                int slot = itemTag.getByte("Slot");
                String id = itemTag.getString("id");
                int count = itemTag.getByte("Count");
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
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            NbtList list = new NbtList();

            if(items != null) {
                for(OPanelItemStack item : items) {
                    if(item == null || item.isEmpty()) continue;
                    NbtCompound itemTag = new NbtCompound();
                    itemTag.putByte("Slot", (byte) item.slot);
                    itemTag.putString("id", item.id);
                    itemTag.putByte("Count", (byte) item.count);
                    list.add(itemTag);
                }
            }

            nbt.put("Inventory", list);
            NbtIo.writeCompressed(nbt, playerDataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        if(item == null) return;
        try {
            NbtCompound nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            NbtList list = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);

            for(int i = list.size() - 1; i >= 0; i--) {
                NbtCompound itemTag = list.getCompound(i);
                if(itemTag.getByte("Slot") == (byte) item.slot) {
                    list.remove(i);
                }
            }

            if(!item.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) item.slot);
                itemTag.putString("id", item.id);
                itemTag.putByte("Count", (byte) item.count);
                list.add(itemTag);
            }

            nbt.put("Inventory", list);
            NbtIo.writeCompressed(nbt, playerDataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}