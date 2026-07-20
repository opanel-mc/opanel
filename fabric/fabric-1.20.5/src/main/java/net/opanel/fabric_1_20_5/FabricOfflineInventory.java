package net.opanel.fabric_1_20_5;

import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;

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
    private NbtList enderNbtList;

    public FabricOfflineInventory(Path playerDataPath) {
        super(playerDataPath);

        try {
            nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            nbtList = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
            enderNbtList = nbt.getList("EnderItems", NbtElement.COMPOUND_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveNbt() throws IOException {
        nbt.put("Inventory", nbtList);
        nbt.put("EnderItems", enderNbtList);
        NbtIo.writeCompressed(nbt, playerDataPath);
    }


    @Override
    public List<OPanelItemStack> getItems(OPanelInventoryType inventoryType) {
        List<OPanelItemStack> items = new ArrayList<>();
        NbtList nbtList = getNbtList(inventoryType);

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
            int count = itemNbt.getByte("count");
            NbtCompound nbt = itemNbt.getCompound("components");
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
        NbtList nbtList = getNbtList(inventoryType);

        try {
            for(int i = nbtList.size() - 1; i >= 0; i--) {
                NbtCompound savedItemNbt = nbtList.getCompound(i);
                if(inventoryType.fromSavedSlot(savedItemNbt.getByte("Slot")) >= 0) nbtList.remove(i);
            }

            for(OPanelItemStack item : items) {
                if(item == null || item.isEmpty() || item.slot < 0 || item.slot >= inventoryType.getSize()) continue;
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
        items.set(item.slot, item);
        setItems(inventoryType, items);
    }

    @Override
    protected NbtCompound toNbt(OPanelInventoryType inventoryType, OPanelItemStack item) throws CommandSyntaxException {
        NbtCompound itemNbt = new NbtCompound();
        itemNbt.putByte("Slot", (byte) inventoryType.toSavedSlot(item.slot));
        itemNbt.putString("id", item.id);
        itemNbt.putByte("count", (byte) item.count);
        if(item.snbt != null) {
            itemNbt.put("components", StringNbtReader.parse(item.snbt));
        }
        return itemNbt;
    }
    
    private NbtList getNbtList(OPanelInventoryType inventoryType) {
        return inventoryType == OPanelInventoryType.ENDER_CHEST ? enderNbtList : nbtList;
    }
}
