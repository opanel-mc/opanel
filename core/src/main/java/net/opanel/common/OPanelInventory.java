package net.opanel.common;

import com.google.gson.Gson;
import net.opanel.utils.Utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public interface OPanelInventory {
    int getSize();
    List<OPanelItemStack> getItems();
    void setItems(List<OPanelItemStack> items) throws Exception;
    void setItem(OPanelItemStack item) throws Exception;

    default OPanelItemStack getItem(int slot) {
        for(OPanelItemStack item : getItems()) {
            if(item.slot == slot) return item;
        }
        return new OPanelItemStack(slot, "minecraft:air", 0, null);
    }

    default String getHash() {
        List<OPanelItemStack> items = getItems();
        items.sort(Comparator.comparingInt(i -> i.slot));
        StringBuilder sb = new StringBuilder(items.size() * 16);
        for(OPanelItemStack item : items) {
            sb.append(item.slot).append('|')
              .append(item.id == null ? "" : item.id).append('|')
              .append(item.count).append('|')
              .append(item.snbt == null ? "" : item.snbt)
              .append(';');
        }
        return Utils.md5(sb.toString());
    }

    default HashMap<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("size", getSize());
        data.put("hash", getHash());
        data.put("items", getItems());
        return data;
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