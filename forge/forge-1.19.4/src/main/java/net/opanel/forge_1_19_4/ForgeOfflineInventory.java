package net.opanel.forge_1_19_4;

import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.opanel.forge_helper.BaseForgeOfflineInventory;
import net.opanel.forge_helper.utils.ForgeUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ForgeOfflineInventory extends BaseForgeOfflineInventory {
    private CompoundTag nbt;
    private ListTag nbtList;
    private ListTag enderNbtList;

    public ForgeOfflineInventory(Path playerDataPath) {
        super(playerDataPath);

        try {
            nbt = NbtIo.readCompressed(playerDataPath.toFile());
            nbtList = nbt.getList("Inventory", ListTag.TAG_COMPOUND);
            enderNbtList = nbt.getList("EnderItems", ListTag.TAG_COMPOUND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveNbt() throws IOException {
        nbt.put("Inventory", nbtList);
        nbt.put("EnderItems", enderNbtList);
        NbtIo.writeCompressed(nbt, playerDataPath.toFile());
    }


    @Override
    public List<OPanelItemStack> getItems(OPanelInventoryType inventoryType) {
        List<OPanelItemStack> items = new ArrayList<>();
        ListTag nbtList = getNbtList(inventoryType);

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
            CompoundTag nbt = itemNbt.getCompound("tag");
            items.add(new OPanelItemStack(slot, id, count, nbt.isEmpty() ? null : nbt.toString()));
            nextSlot = slot + 1;
        }

        if(nextSlot <= 35) {
            for(int i = nextSlot; i < 36; i++) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
            }
        }

        return OPanelInventory.normalizeSavedItems(inventoryType, items);
    }

    @Override
    public void setItems(OPanelInventoryType inventoryType, List<OPanelItemStack> items) throws CommandSyntaxException {
        ListTag nbtList = getNbtList(inventoryType);

        try {
            for(int i = nbtList.size() - 1; i >= 0; i--) {
                CompoundTag savedItemNbt = nbtList.getCompound(i);
                if(inventoryType.fromSavedSlot(savedItemNbt.getByte("Slot")) >= 0) nbtList.remove(i);
            }

            for(OPanelItemStack item : items) {
                if(item == null || item.isEmpty() || item.slot() < 0 || item.slot() >= inventoryType.getSize()) continue;
                nbtList.add(toNbt(inventoryType, item));
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelInventoryType inventoryType, OPanelItemStack item) throws CommandSyntaxException {
        List<OPanelItemStack> items = getItems(inventoryType);
        items.set(item.slot(), item);
        setItems(inventoryType, items);
    }

    @Override
    protected CompoundTag toNbt(OPanelInventoryType inventoryType, OPanelItemStack item) throws CommandSyntaxException {
        CompoundTag itemNbt = new CompoundTag();
        itemNbt.putByte("Slot", (byte) inventoryType.toSavedSlot(item.slot()));
        itemNbt.putString("id", item.id());
        itemNbt.putByte("Count", (byte) item.count());
        if(item.snbt() != null) {
            itemNbt.put("tag", TagParser.parseTag(item.snbt()));
        }
        return itemNbt;
    }

    private ListTag getNbtList(OPanelInventoryType inventoryType) {
        return inventoryType == OPanelInventoryType.ENDER_CHEST ? enderNbtList : nbtList;
    }
}
