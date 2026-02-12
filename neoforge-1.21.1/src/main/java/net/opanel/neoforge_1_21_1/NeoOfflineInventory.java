package net.opanel.neoforge_1_21_1;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;
import net.opanel.common.OPanelInventory;
import net.opanel.neoforge_1_21_1.utils.NeoUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NeoOfflineInventory implements OPanelInventory {
    private final Path playerDataPath;
    private CompoundTag nbt;
    private ListTag nbtList;

    public NeoOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;

        try {
            nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
            nbtList = nbt.getList("Inventory", ListTag.TAG_COMPOUND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            CompoundTag itemNbt = nbtList.getCompound(i);
            int slot = itemNbt.getByte("Slot");
            if(slot > nextSlot) {
                for(int j = nextSlot; j < slot; j++) {
                    items.add(new OPanelItemStack(j, "minecraft:air", 0, null));
                }
            }

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte("count");
            CompoundTag nbt = itemNbt.getCompound("components");
            items.add(new OPanelItemStack(slot, id, count, nbt.isEmpty() ? null : nbt.toString()));
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
    public void setItems(List<OPanelItemStack> items) throws CommandSyntaxException {
        if(nbtList.isEmpty()) return;

        try {
            nbtList.clear();

            for(OPanelItemStack item : items) {
                if(item == null || item.isEmpty()) continue;
                nbtList.add(toNbt(item));
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) throws CommandSyntaxException {
        try {
            if(nbtList == null) return;

            // Insert into empty list or to the last
            if(nbtList.isEmpty() || item.slot > nbtList.getCompound(nbtList.size() - 1).getByte("Slot")) {
                nbtList.add(toNbt(item));
                saveNbt();
                return;
            }

            for(int i = 0; i < nbtList.size(); i++) {
                CompoundTag itemNbt = nbtList.getCompound(i);
                int slot = itemNbt.getByte("Slot");

                // Insert into an empty slot
                if(slot > item.slot) {
                    NeoUtils.addCompoundToNBTList(nbtList, toNbt(item), i);
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
                    if(item.snbt != null) {
                        itemNbt.put("components", TagParser.parseTag(item.snbt));
                    }
                    break;
                }
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CompoundTag toNbt(OPanelItemStack item) throws CommandSyntaxException {
        CompoundTag itemNbt = new CompoundTag();
        itemNbt.putByte("Slot", (byte) item.slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("count", (byte) item.count);
        if(item.snbt != null) {
            itemNbt.put("components", TagParser.parseTag(item.snbt));
        }
        return itemNbt;
    }
}
