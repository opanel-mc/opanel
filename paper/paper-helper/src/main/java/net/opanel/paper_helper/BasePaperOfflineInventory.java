package net.opanel.paper_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import net.opanel.paper_helper.utils.PaperUtils;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class BasePaperOfflineInventory implements OPanelInventory {
    protected final Path playerDataPath;
    protected ReadWriteNBT nbt;

    private final String KEY_OF_COUNT = keyOfCount();
    private final String KEY_OF_NBT = keyOfNBT();

    public BasePaperOfflineInventory(Path playerDataPath) {
        this.playerDataPath = playerDataPath;
        try {
            nbt = NBT.readFile(playerDataPath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * In MC versions < 1.20.5, the nbt key that represents the item amount is "Count",
     * while in MC versions >= 1.20.5, that key is changed to "count".
     */
    protected abstract String keyOfCount();
    protected abstract String keyOfNBT();

    protected void saveNbt() throws IOException {
        NBT.writeFile(playerDataPath.toFile(), nbt);
    }

    @Override
    public List<OPanelItemStack> getItems(OPanelInventoryType inventoryType) {
        List<OPanelItemStack> items = OPanelInventory.createEmptyItems(inventoryType);
        if(inventoryType == OPanelInventoryType.EQUIPMENTS && usesEquipmentTag()) {
            ReadWriteNBT equipmentNbt = nbt.getCompound("equipment");
            if(equipmentNbt == null) return items;

            for(int slot = 0; slot < inventoryType.getSize(); slot++) {
                ReadWriteNBT itemNbt = equipmentNbt.getCompound(OPanelInventoryType.getEquipmentSlotName(slot));
                if(itemNbt != null) items.set(slot, fromNbt(slot, itemNbt));
            }
            return items;
        }

        ReadWriteNBTCompoundList list = getNbtList(inventoryType);
        for(ReadWriteNBT itemNbt : list) {
            int slot = toLogicalSlot(inventoryType, itemNbt.getByte("Slot"));
            if(slot < 0 || slot >= inventoryType.getSize()) continue;

            items.set(slot, fromNbt(slot, itemNbt));
        }

        return items;
    }

    @Override
    public void setItems(OPanelInventoryType inventoryType, List<OPanelItemStack> items) throws NbtApiException {
        try {
            if(inventoryType == OPanelInventoryType.EQUIPMENTS && usesEquipmentTag()) {
                ReadWriteNBT equipmentNbt = nbt.getOrCreateCompound("equipment");
                for(int slot = 0; slot < inventoryType.getSize(); slot++) {
                    equipmentNbt.removeKey(OPanelInventoryType.getEquipmentSlotName(slot));
                }
                for(OPanelItemStack item : items) {
                    if(item == null || item.isEmpty() || item.slot < 0 || item.slot >= inventoryType.getSize()) continue;
                    equipmentNbt.set(
                        OPanelInventoryType.getEquipmentSlotName(item.slot),
                        toNbt(inventoryType, item),
                        NBTHandlers.STORE_READWRITE_TAG
                    );
                }
                if(equipmentNbt.getKeys().isEmpty()) nbt.removeKey("equipment");
                saveNbt();
                return;
            }

            ReadWriteNBTCompoundList list = getNbtList(inventoryType);
            for(int i = list.size() - 1; i >= 0; i--) {
                if(toLogicalSlot(inventoryType, list.get(i).getByte("Slot")) >= 0) list.remove(i);
            }

            for(OPanelItemStack item : items) {
                if(item == null || item.isEmpty() || item.slot < 0 || item.slot >= inventoryType.getSize()) continue;
                list.addCompound(toNbt(inventoryType, item));
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelInventoryType inventoryType, OPanelItemStack item) throws NbtApiException {
        List<OPanelItemStack> items = getItems(inventoryType);
        items.set(item.slot, item);
        setItems(inventoryType, items);
    }

    protected ReadWriteNBT toNbt(OPanelInventoryType inventoryType, OPanelItemStack item) throws NbtApiException {
        ReadWriteNBT itemNbt = NBT.createNBTObject();
        if(inventoryType != OPanelInventoryType.EQUIPMENTS || !usesEquipmentTag()) {
            itemNbt.setByte("Slot", (byte) inventoryType.toSavedSlot(item.slot));
        }
        itemNbt.setString("id", item.id);
        itemNbt.setByte(KEY_OF_COUNT, (byte) item.count);
        if(item.snbt != null) {
            itemNbt.set(KEY_OF_NBT, NBT.parseNBT(item.snbt), NBTHandlers.STORE_READWRITE_TAG);
        }
        return itemNbt;
    }

    private ReadWriteNBTCompoundList getNbtList(OPanelInventoryType inventoryType) {
        return nbt.getCompoundList(inventoryType == OPanelInventoryType.ENDER_CHEST ? "EnderItems" : "Inventory");
    }

    private int toLogicalSlot(OPanelInventoryType inventoryType, int nativeSlot) {
        return inventoryType.fromSavedSlot(nativeSlot);
    }

    private OPanelItemStack fromNbt(int slot, ReadWriteNBT itemNbt) {
        ReadWriteNBT components = itemNbt.getCompound(KEY_OF_NBT);
        return new OPanelItemStack(
            slot,
            itemNbt.getString("id"),
            itemNbt.getByte(KEY_OF_COUNT),
            components == null ? null : components.toString()
        );
    }

    protected abstract boolean usesEquipmentTag();
}
