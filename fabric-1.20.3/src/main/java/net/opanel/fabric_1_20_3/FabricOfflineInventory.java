package net.opanel.fabric_1_20_3;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.opanel.fabric_helper.BaseFabricOfflineInventory;
import net.opanel.fabric_helper.FabricUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricOfflineInventory extends BaseFabricOfflineInventory {
    private NbtCompound nbt;
    private NbtList nbtList;

    /**
     * In MC versions < 1.20.5, the nbt key that represents the item amount is "Count",
     * while in MC versions >= 1.20.5, that key is changed to "count".
     */
    private final String KEY_OF_COUNT;

    public FabricOfflineInventory(MinecraftServer server, Path playerDataPath) {
        super(playerDataPath);

        final String version = server.getVersion();
        KEY_OF_COUNT = version.equals("1.20.5") || version.equals("1.20.6") ? "count" : "Count";

        try {
            nbt = NbtIo.readCompressed(playerDataPath, NbtTagSizeTracker.ofUnlimitedBytes());
            nbtList = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
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
            NbtCompound itemNbt = nbtList.getCompound(i);
            int slot = itemNbt.getByte("Slot");
            if(slot > nextSlot) {
                for(int j = nextSlot; j < slot; j++) {
                    items.add(new OPanelItemStack(j, "minecraft:air", 0, null));
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
        if(nbtList.isEmpty()) return;

        try {
            nbtList.clear();

            for(OPanelItemStack item : items) {
                if(item == null || item.isEmpty()) continue;
                NbtCompound itemNbt = new NbtCompound();
                itemNbt.putByte("Slot", (byte) item.slot);
                itemNbt.putString("id", item.id);
                itemNbt.putByte(KEY_OF_COUNT, (byte) item.count);
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
                NbtCompound newItemNbt = new NbtCompound();
                newItemNbt.putByte("Slot", (byte) item.slot);
                newItemNbt.putString("id", item.id);
                newItemNbt.putByte(KEY_OF_COUNT, (byte) item.count);
                nbtList.add(newItemNbt);
                saveNbt();
                return;
            }

            for(int i = 0; i < nbtList.size(); i++) {
                NbtCompound itemNbt = nbtList.getCompound(i);
                int slot = itemNbt.getByte("Slot");

                // Insert into an empty slot
                if(slot > item.slot) {
                    NbtCompound newItemNbt = new NbtCompound();
                    newItemNbt.putByte("Slot", (byte) item.slot);
                    newItemNbt.putString("id", item.id);
                    newItemNbt.putByte(KEY_OF_COUNT, (byte) item.count);
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
                    itemNbt.putByte(KEY_OF_COUNT, (byte) item.count);
                    break;
                }
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
