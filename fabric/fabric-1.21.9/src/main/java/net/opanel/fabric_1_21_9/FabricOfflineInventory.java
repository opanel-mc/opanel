package net.opanel.fabric_1_21_9;

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
import java.util.Optional;

public class FabricOfflineInventory extends BaseFabricOfflineInventory {
    private NbtCompound nbt;
    private NbtList nbtList;
    private NbtList enderNbtList;
    private NbtCompound equipmentNbt;

    public FabricOfflineInventory(Path playerDataPath) {
        super(playerDataPath);

        try {
            nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.ofUnlimitedBytes());
            nbtList = nbt.getListOrEmpty("Inventory");
            enderNbtList = nbt.getListOrEmpty("EnderItems");
            equipmentNbt = nbt.getCompound("equipment").orElseGet(NbtCompound::new);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveNbt() throws IOException {
        nbt.put("Inventory", nbtList);
        nbt.put("EnderItems", enderNbtList);
        nbt.put("equipment", equipmentNbt);
        NbtIo.writeCompressed(nbt, playerDataPath);
    }

    @Override
    public List<OPanelItemStack> getItems(OPanelInventoryType inventoryType) {
        if(inventoryType == OPanelInventoryType.EQUIPMENTS) return getEquipmentItems();

        List<OPanelItemStack> items = new ArrayList<>();
        NbtList nbtList = getNbtList(inventoryType);

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
            Optional<NbtCompound> nbt = itemNbt.getCompound("components");
            items.add(new OPanelItemStack(slot, id, count, nbt.map(NbtCompound::toString).orElse(null)));
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
        if(inventoryType == OPanelInventoryType.EQUIPMENTS) {
            try {
                for(int slot = 0; slot < inventoryType.getSize(); slot++) {
                    equipmentNbt.remove(OPanelInventoryType.getEquipmentSlotName(slot));
                }
                for(OPanelItemStack item : items) {
                    if(item == null || item.isEmpty() || item.slot < 0 || item.slot >= inventoryType.getSize()) continue;
                    equipmentNbt.put(OPanelInventoryType.getEquipmentSlotName(item.slot), toNbt(inventoryType, item));
                }
                saveNbt();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        NbtList nbtList = getNbtList(inventoryType);

        try {
            for(int i = nbtList.size() - 1; i >= 0; i--) {
                NbtCompound savedItemNbt = nbtList.getCompoundOrEmpty(i);
                if(inventoryType.fromSavedSlot(savedItemNbt.getByte("Slot", (byte) 0)) >= 0) nbtList.remove(i);
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
        if(inventoryType != OPanelInventoryType.EQUIPMENTS) {
            itemNbt.putByte("Slot", (byte) inventoryType.toSavedSlot(item.slot));
        }
        itemNbt.putString("id", item.id);
        itemNbt.putByte("count", (byte) item.count);
        if(item.snbt != null) {
            itemNbt.put("components", StringNbtReader.readCompound(item.snbt));
        }
        return itemNbt;
    }
    
    private NbtList getNbtList(OPanelInventoryType inventoryType) {
        return inventoryType == OPanelInventoryType.ENDER_CHEST ? enderNbtList : nbtList;
    }

    private List<OPanelItemStack> getEquipmentItems() {
        List<OPanelItemStack> items = OPanelInventory.createEmptyItems(OPanelInventoryType.EQUIPMENTS);
        for(int slot = 0; slot < OPanelInventoryType.EQUIPMENTS.getSize(); slot++) {
            Optional<NbtCompound> itemNbt = equipmentNbt.getCompound(OPanelInventoryType.getEquipmentSlotName(slot));
            if(itemNbt.isEmpty()) continue;
            NbtCompound value = itemNbt.get();
            Optional<NbtCompound> components = value.getCompound("components");
            items.set(slot, new OPanelItemStack(
                slot,
                value.getString("id", "minecraft:air"),
                value.getByte("count", (byte) 0),
                components.map(NbtCompound::toString).orElse(null)
            ));
        }
        return items;
    }

}
