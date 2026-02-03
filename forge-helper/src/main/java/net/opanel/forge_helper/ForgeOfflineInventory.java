package net.opanel.forge_helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.opanel.common.OPanelInventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ForgeOfflineInventory implements OPanelInventory {
    private final Path playerDataPath;

    public ForgeOfflineInventory(Path playerDataPath) {
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
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
            ListTag list = nbt.getList("Inventory", Tag.TAG_COMPOUND);
            for(int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
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
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
            ListTag list = new ListTag();

            if(items != null) {
                for(OPanelItemStack item : items) {
                    if(item == null || item.isEmpty()) continue;
                    CompoundTag itemTag = new CompoundTag();
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
            CompoundTag nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
            ListTag list = nbt.getList("Inventory", Tag.TAG_COMPOUND);

            for(int i = list.size() - 1; i >= 0; i--) {
                CompoundTag itemTag = list.getCompound(i);
                if(itemTag.getByte("Slot") == (byte) item.slot) {
                    list.remove(i);
                }
            }

            if(!item.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
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