package net.opanel.forge_1_21_5;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.opanel.forge_helper.BaseForgeOfflineInventory;
import net.opanel.forge_helper.ForgeUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ForgeOfflineInventory extends BaseForgeOfflineInventory {
    private CompoundTag nbt;
    private ListTag nbtList;

    public ForgeOfflineInventory(Path playerDataPath) {
        super(playerDataPath);

        try {
            nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
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
            CompoundTag itemNbt = nbtList.getCompoundOrEmpty(i);
            int slot = itemNbt.getByteOr("Slot", (byte) 0);
            if(slot > nextSlot) {
                for(int j = nextSlot; j < slot; j++) {
                    items.add(new OPanelItemStack(j, "minecraft:air", 0, null));
                }
            }

            String id = itemNbt.getStringOr("id", "minecraft:air");
            int count = itemNbt.getByteOr("count", (byte) 0);
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
                CompoundTag itemNbt = new CompoundTag();
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
            if(item.slot > nbtList.getCompoundOrEmpty(nbtList.size() - 1).getByteOr("Slot", (byte) 0)) {
                CompoundTag newItemNbt = new CompoundTag();
                newItemNbt.putByte("Slot", (byte) item.slot);
                newItemNbt.putString("id", item.id);
                newItemNbt.putByte("count", (byte) item.count);
                nbtList.add(newItemNbt);
                saveNbt();
                return;
            }

            for(int i = 0; i < nbtList.size(); i++) {
                CompoundTag itemNbt = nbtList.getCompoundOrEmpty(i);
                int slot = itemNbt.getByteOr("Slot", (byte) 0);

                // Insert into an empty slot
                if(slot > item.slot) {
                    CompoundTag newItemNbt = new CompoundTag();
                    newItemNbt.putByte("Slot", (byte) item.slot);
                    newItemNbt.putString("id", item.id);
                    newItemNbt.putByte("count", (byte) item.count);
                    ForgeUtils.addCompoundToNBTList(nbtList, newItemNbt, i);
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
