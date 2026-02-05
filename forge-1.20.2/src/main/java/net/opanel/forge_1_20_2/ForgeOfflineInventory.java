package net.opanel.forge_1_20_2;

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
            nbt = NbtIo.readCompressed(playerDataPath.toFile());
            nbtList = nbt.getList("Inventory", ListTag.TAG_COMPOUND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveNbt() throws IOException {
        nbt.put("Inventory", nbtList);
        NbtIo.writeCompressed(nbt, playerDataPath.toFile());
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
            CompoundTag itemNbt = nbtList.getCompound(i);
            int slot = itemNbt.getByte("Slot");
            if(slot > nextSlot) {
                for(int j = nextSlot; j < slot; j++) {
                    items.add(new OPanelItemStack(j, "minecraft:air", 0, null));
                }
            }

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte("Count");
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
                itemNbt.putByte("Count", (byte) item.count);
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
            if(item.slot > nbtList.getCompound(nbtList.size() - 1).getByte("Slot")) {
                CompoundTag newItemNbt = new CompoundTag();
                newItemNbt.putByte("Slot", (byte) item.slot);
                newItemNbt.putString("id", item.id);
                newItemNbt.putByte("Count", (byte) item.count);
                nbtList.add(newItemNbt);
                saveNbt();
                return;
            }

            for(int i = 0; i < nbtList.size(); i++) {
                CompoundTag itemNbt = nbtList.getCompound(i);
                int slot = itemNbt.getByte("Slot");

                // Insert into an empty slot
                if(slot > item.slot) {
                    CompoundTag newItemNbt = new CompoundTag();
                    newItemNbt.putByte("Slot", (byte) item.slot);
                    newItemNbt.putString("id", item.id);
                    newItemNbt.putByte("Count", (byte) item.count);
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
                    itemNbt.putByte("Count", (byte) item.count);
                    break;
                }
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
