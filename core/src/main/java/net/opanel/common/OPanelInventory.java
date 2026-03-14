package net.opanel.common;

import com.google.gson.Gson;
import net.opanel.utils.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
        return getHashOfItems(getItems());
    }

    default int getEnderSize() {
        return 0;
    }

    default List<OPanelItemStack> getEnderItems() {
        return Collections.emptyList();
    }

    default void setEnderItem(OPanelItemStack item) throws Exception {
        throw new UnsupportedOperationException("Ender chest editing is unsupported.");
    }

    default boolean canReadEnderChest() {
        return !getEnderItems().isEmpty();
    }

    default boolean canWriteEnderChest() {
        return false;
    }

    default String getEnderHash() {
        return getHashOfItems(getEnderItems());
    }

    default HashMap<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("size", getSize());
        data.put("hash", getHash());
        data.put("items", getItems());
        data.put("enderSize", getEnderSize());
        data.put("enderHash", getEnderHash());
        data.put("enderItems", getEnderItems());

        HashMap<String, Object> capabilities = new HashMap<>();
        capabilities.put("readEnderChest", canReadEnderChest());
        capabilities.put("writeEnderChest", canWriteEnderChest());
        data.put("capabilities", capabilities);
        return data;
    }

    static String getHashOfItems(List<OPanelItemStack> sourceItems) {
        List<OPanelItemStack> items = sourceItems.stream()
                .map(item -> new OPanelItemStack(item.slot, item.id, item.count, item.snbt, item.container))
                .sorted(Comparator.comparingInt(i -> i.slot))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder(items.size() * 16);
        for(OPanelItemStack item : items) {
            sb.append(item.slot).append('|')
              .append(item.id == null ? "" : item.id).append('|')
              .append(item.count).append('|')
              .append(item.snbt == null ? "" : item.snbt)
              .append('|')
              .append(item.container == null ? "" : item.container)
              .append(';');
        }
        return Utils.md5(sb.toString());
    }

    class OPanelItemStack {
        public int slot;
        public String id;
        public int count;
        public String snbt;
        public String container;

        public OPanelItemStack(int slot, String id, int count, String snbt) {
            this(slot, id, count, snbt, null);
        }

        public OPanelItemStack(int slot, String id, int count, String snbt, String container) {
            this.slot = slot;
            this.id = id;
            this.count = count;
            this.snbt = snbt;
            this.container = container;
        }

        public boolean isEmpty() {
            return count <= 0 || id == null || id.equals("minecraft:air");
        }
    }
}
