package net.opanel.fabric_1_20;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;
import net.opanel.fabric_helper.BaseFabricOfflineInventory;
import net.opanel.fabric_helper.utils.FabricUtils;

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
            nbt = NbtIo.readCompressed(playerDataPath.toFile());
            nbtList = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
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
            NbtCompound itemNbt = nbtList.getCompound(i);
            int slot = itemNbt.getByte("Slot");
            if(slot > nextSlot) {
                for(int j = nextSlot; j < slot; j++) {
                    items.add(new OPanelItemStack(j, "minecraft:air", 0, null));
                }
            }

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte("Count");
            NbtCompound nbt = itemNbt.getCompound("tag");
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
                NbtCompound itemNbt = nbtList.getCompound(i);
                int slot = itemNbt.getByte("Slot");

                // Insert into an empty slot
                if(slot > item.slot) {
                    FabricUtils.addCompoundToNBTList(nbtList, toNbt(item), i);
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
                    if(item.snbt != null) {
                        itemNbt.put("tag", StringNbtReader.parse(item.snbt));
                    }
                    break;
                }
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected NbtCompound toNbt(OPanelItemStack item) throws CommandSyntaxException {
        NbtCompound itemNbt = new NbtCompound();
        itemNbt.putByte("Slot", (byte) item.slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("Count", (byte) item.count);
        if(item.snbt != null) {
            itemNbt.put("tag", StringNbtReader.parse(item.snbt));
        }
        return itemNbt;
    }
}