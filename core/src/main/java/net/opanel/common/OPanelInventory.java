package net.opanel.common;

import net.opanel.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public interface OPanelInventory {
    default int getSize(OPanelInventoryType inventoryType) {
        return inventoryType.getSize();
    }

    List<OPanelItemStack> getItems(OPanelInventoryType inventoryType);
    void setItems(OPanelInventoryType inventoryType, List<OPanelItemStack> items) throws Exception;
    void setItem(OPanelInventoryType inventoryType, OPanelItemStack item) throws Exception;

    default OPanelItemStack getItem(OPanelInventoryType inventoryType, int slot) {
        for(OPanelItemStack item : getItems(inventoryType)) {
            if(item.slot == slot) return item;
        }
        return new OPanelItemStack(slot, "minecraft:air", 0, null);
    }

    default String getHash() {
        StringBuilder sb = new StringBuilder();
        for(OPanelInventoryType inventoryType : OPanelInventoryType.values()) {
            List<OPanelItemStack> items = new ArrayList<>(getItems(inventoryType));
            items.sort(Comparator.comparingInt(i -> i.slot));
            sb.append(inventoryType.getName()).append('{');
            for(OPanelItemStack item : items) {
                sb.append(item.slot).append('|')
                  .append(item.id == null ? "" : item.id).append('|')
                  .append(item.count).append('|')
                  .append(item.snbt == null ? "" : item.snbt)
                  .append(';');
            }
            sb.append('}');
        }
        return Utils.md5(sb.toString());
    }

    default HashMap<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("hash", getHash());
        for(OPanelInventoryType inventoryType : OPanelInventoryType.values()) {
            HashMap<String, Object> inventoryData = new HashMap<>();
            inventoryData.put("size", getSize(inventoryType));
            inventoryData.put("items", getItems(inventoryType));
            data.put(inventoryType.getName(), inventoryData);
        }
        return data;
    }

    static List<OPanelItemStack> createEmptyItems(OPanelInventoryType inventoryType) {
        List<OPanelItemStack> items = new ArrayList<>(inventoryType.getSize());
        for(int slot = 0; slot < inventoryType.getSize(); slot++) {
            items.add(new OPanelItemStack(slot, "minecraft:air", 0, null));
        }
        return items;
    }

    static List<OPanelItemStack> normalizeSavedItems(
        OPanelInventoryType inventoryType,
        List<OPanelItemStack> savedItems
    ) {
        List<OPanelItemStack> items = createEmptyItems(inventoryType);
        for(OPanelItemStack savedItem : savedItems) {
            if(savedItem == null || savedItem.isEmpty()) continue;
            int logicalSlot = inventoryType.fromSavedSlot(savedItem.slot);
            if(logicalSlot < 0) continue;
            items.set(logicalSlot, new OPanelItemStack(
                logicalSlot,
                savedItem.id,
                savedItem.count,
                savedItem.snbt
            ));
        }
        return items;
    }

    class OPanelItemStack {
        public int slot;
        public String id;
        public int count;
        public String snbt;

        public OPanelItemStack(int slot, String id, int count, String snbt) {
            this.slot = slot;
            this.id = id;
            this.count = count;
            this.snbt = snbt;
        }

        public boolean isEmpty() {
            return count <= 0 || id == null || id.equals("minecraft:air");
        }
    }
}
