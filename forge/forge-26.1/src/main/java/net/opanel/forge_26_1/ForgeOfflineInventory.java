package net.opanel.forge_26_1;

import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;

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
    private ListTag enderNbtList;
    private CompoundTag equipmentNbt;

    public ForgeOfflineInventory(Path playerDataPath) {
        super(playerDataPath);

        try {
            nbt = NbtIo.readCompressed(playerDataPath, NbtAccounter.unlimitedHeap());
            nbtList = nbt.getListOrEmpty("Inventory");
            enderNbtList = nbt.getListOrEmpty("EnderItems");
            equipmentNbt = nbt.getCompound("equipment").orElseGet(CompoundTag::new);
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
        ListTag nbtList = getNbtList(inventoryType);

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
                    if(item == null || item.isEmpty() || item.slot() < 0 || item.slot() >= inventoryType.getSize()) continue;
                    equipmentNbt.put(OPanelInventoryType.getEquipmentSlotName(item.slot()), toNbt(inventoryType, item));
                }
                saveNbt();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        ListTag nbtList = getNbtList(inventoryType);

        try {
            for(int i = nbtList.size() - 1; i >= 0; i--) {
                CompoundTag savedItemNbt = nbtList.getCompoundOrEmpty(i);
                if(inventoryType.fromSavedSlot(savedItemNbt.getByteOr("Slot", (byte) 0)) >= 0) nbtList.remove(i);
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
        if(inventoryType != OPanelInventoryType.EQUIPMENTS) {
            itemNbt.putByte("Slot", (byte) inventoryType.toSavedSlot(item.slot()));
        }
        itemNbt.putString("id", item.id());
        itemNbt.putByte("count", (byte) item.count());
        if(item.snbt() != null) {
            itemNbt.put("components", TagParser.parseCompoundFully(item.snbt()));
        }
        return itemNbt;
    }

    private ListTag getNbtList(OPanelInventoryType inventoryType) {
        return inventoryType == OPanelInventoryType.ENDER_CHEST ? enderNbtList : nbtList;
    }

    private List<OPanelItemStack> getEquipmentItems() {
        List<OPanelItemStack> items = OPanelInventory.createEmptyItems(OPanelInventoryType.EQUIPMENTS);
        for(int slot = 0; slot < OPanelInventoryType.EQUIPMENTS.getSize(); slot++) {
            Optional<CompoundTag> itemNbt = equipmentNbt.getCompound(OPanelInventoryType.getEquipmentSlotName(slot));
            if(itemNbt.isEmpty()) continue;
            CompoundTag value = itemNbt.get();
            Optional<CompoundTag> components = value.getCompound("components");
            items.set(slot, new OPanelItemStack(
                slot,
                value.getStringOr("id", "minecraft:air"),
                value.getByteOr("count", (byte) 0),
                components.map(CompoundTag::toString).orElse(null)
            ));
        }
        return items;
    }

}
