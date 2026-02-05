package net.opanel.fabric_1_21_9;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.opanel.fabric_helper.BaseFabricOfflineInventory;
import net.opanel.fabric_helper.FabricUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricOfflineInventory extends BaseFabricOfflineInventory {
    private NbtCompound nbt;
    private NbtList nbtList;

    public FabricOfflineInventory(Path playerDataPath) {
        super(playerDataPath);

        try {
            nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            nbtList = nbt.getListOrEmpty("Inventory");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveNbt() throws IOException {
        nbt.put("Inventory", nbtList);
        NbtIo.writeCompressed(nbt, playerDataPath);
    }

    @Override
    public int getSize() {
        return nbtList.size();
    }

    @Override
    public List<OPanelItemStack> getItems() {
        List<OPanelItemStack> items = new ArrayList<>();

        int nextSlot = 0;
        for(int i = 0; i < nbtList.size(); i++) {
            NbtCompound itemNbt = nbtList.getCompoundOrEmpty(i);
            int slot = itemNbt.getByte("Slot", (byte) 0);
            if(slot > nextSlot) {
                for(int j = nextSlot; j < slot; j++) {
                    items.add(new OPanelItemStack(j, "minecraft:air", 0, null));
                }
            }

            String id = itemNbt.getString("id", "minecraft:air");
            int count = itemNbt.getByte("count", (byte) 0);
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
        if(nbtList.isEmpty()) return;

        try {
            nbtList.clear();

            for(OPanelItemStack item : items) {
                if(item == null || item.isEmpty()) continue;
                NbtCompound itemNbt = new NbtCompound();
                itemNbt.putByte("Slot", (byte) item.slot);
                itemNbt.putString("id", item.id);
                itemNbt.putByte("count", (byte) item.count);
                nbtList.add(itemNbt);
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        try {
            if(nbtList == null) return;

            // Insert to the last
            if(item.slot > nbtList.getCompoundOrEmpty(nbtList.size() - 1).getByte("Slot", (byte) 0)) {
                NbtCompound newItemNbt = new NbtCompound();
                newItemNbt.putByte("Slot", (byte) item.slot);
                newItemNbt.putString("id", item.id);
                newItemNbt.putByte("count", (byte) item.count);
                nbtList.add(newItemNbt);
                saveNbt();
                return;
            }

            for(int i = 0; i < nbtList.size(); i++) {
                NbtCompound itemNbt = nbtList.getCompoundOrEmpty(i);
                int slot = itemNbt.getByte("Slot", (byte) 0);

                // Insert into an empty slot
                if(slot > item.slot) {
                    NbtCompound newItemNbt = new NbtCompound();
                    newItemNbt.putByte("Slot", (byte) item.slot);
                    newItemNbt.putString("id", item.id);
                    newItemNbt.putByte("count", (byte) item.count);
                    FabricUtils.addCompoundToNBTList(nbtList, newItemNbt, i);
                    break;
                }
                // Remove the item
                if(slot == item.slot && item.isEmpty()) {
                    nbtList.remove(i);
                    break;
                }
                // Update the slot item
                if(slot == item.slot) {
                    itemNbt.putString("id", item.id);
                    itemNbt.putByte("count", (byte) item.count);
                    break;
                }
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}