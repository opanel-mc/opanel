package net.opanel.forge_1_21_8;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;
import net.opanel.forge_helper.BaseForgeOfflineInventory;
import net.opanel.forge_helper.utils.ForgeUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            Optional<CompoundTag> nbt = itemNbt.getCompound("components");
            items.add(new OPanelItemStack(slot, id, count, nbt.map(CompoundTag::toString).orElse(null)));
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
            if(nbtList.isEmpty() || item.slot > nbtList.getCompoundOrEmpty(nbtList.size() - 1).getByteOr("Slot", (byte) 0)) {
                nbtList.add(toNbt(item));
                saveNbt();
                return;
            }

            for(int i = 0; i < nbtList.size(); i++) {
                CompoundTag itemNbt = nbtList.getCompoundOrEmpty(i);
                int slot = itemNbt.getByteOr("Slot", (byte) 0);

                // Insert into an empty slot
                if(slot > item.slot) {
                    ForgeUtils.addCompoundToNBTList(nbtList, toNbt(item), i);
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
                        itemNbt.put("components", TagParser.parseCompoundFully(item.snbt));
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
    protected CompoundTag toNbt(OPanelItemStack item) throws CommandSyntaxException {
        CompoundTag itemNbt = new CompoundTag();
        itemNbt.putByte("Slot", (byte) item.slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("count", (byte) item.count);
        if(item.snbt != null) {
            itemNbt.put("components", TagParser.parseCompoundFully(item.snbt));
        }
        return itemNbt;
    }
}
