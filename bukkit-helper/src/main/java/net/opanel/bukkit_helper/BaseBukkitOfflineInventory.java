package net.opanel.bukkit_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import net.opanel.common.OPanelInventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseBukkitOfflineInventory implements OPanelInventory {
    private static final int MAIN_LAST_SLOT = 35;
    private static final int PLAYER_SLOT_COUNT = 41;
    private static final int ENDER_SLOT_COUNT = 27;
    private static final int ARMOR_RAW_MIN = 100;
    private static final int ARMOR_RAW_MAX = 103;
    private static final int OFFHAND_RAW_SLOT = 150;

    protected final Path playerDataPath;
    protected ReadWriteNBT nbt;

    private final String KEY_OF_COUNT = keyOfCount();
    private final String KEY_OF_NBT = keyOfNBT();

    public BaseBukkitOfflineInventory(Path playerDataPath) {
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

    private static int normalizeRawSlot(int rawSlot) {
        return rawSlot & 0xFF;
    }

    private static int toPanelSlot(int rawSlot) {
        final int normalized = normalizeRawSlot(rawSlot);
        if(normalized <= MAIN_LAST_SLOT) return normalized;
        if(normalized >= ARMOR_RAW_MIN && normalized <= ARMOR_RAW_MAX) {
            return normalized - 64;
        }
        if(normalized == OFFHAND_RAW_SLOT) return 40;
        return -1;
    }

    private static int toRawSlot(int panelSlot) {
        if(panelSlot >= 0 && panelSlot <= MAIN_LAST_SLOT) return panelSlot;
        if(panelSlot >= 36 && panelSlot <= 39) return panelSlot + 64;
        if(panelSlot == 40) return OFFHAND_RAW_SLOT;
        return -1;
    }

    private static boolean isValidMainSlot(int slot) {
        return slot >= 0 && slot < PLAYER_SLOT_COUNT;
    }

    @Override
    public int getSize() {
        return PLAYER_SLOT_COUNT;
    }

    @Override
    public List<OPanelItemStack> getItems() {
        List<OPanelItemStack> items = new ArrayList<>(PLAYER_SLOT_COUNT);
        for(int i = 0; i < PLAYER_SLOT_COUNT; i++) {
            items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
        }

        ReadWriteNBTCompoundList list = nbt.getCompoundList("Inventory");
        if(list == null) return items;

        for(ReadWriteNBT itemNbt : list) {
            int panelSlot = toPanelSlot(itemNbt.getByte("Slot"));
            if(!isValidMainSlot(panelSlot)) continue;

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte(KEY_OF_COUNT);
            ReadWriteNBT components = itemNbt.getCompound(KEY_OF_NBT);
            items.set(panelSlot, new OPanelItemStack(
                panelSlot,
                id,
                count,
                components == null ? null : components.toString()
            ));
        }

        return items;
    }

    @Override
    public int getEnderSize() {
        return ENDER_SLOT_COUNT;
    }

    @Override
    public List<OPanelItemStack> getEnderItems() {
        List<OPanelItemStack> items = new ArrayList<>(ENDER_SLOT_COUNT);
        for(int i = 0; i < ENDER_SLOT_COUNT; i++) {
            items.add(new OPanelItemStack(i, "minecraft:air", 0, null, "ender"));
        }

        ReadWriteNBTCompoundList list = nbt.getCompoundList("EnderItems");
        if(list == null) return items;

        for(ReadWriteNBT itemNbt : list) {
            int slot = normalizeRawSlot(itemNbt.getByte("Slot"));
            if(slot < 0 || slot >= ENDER_SLOT_COUNT) continue;

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte(KEY_OF_COUNT);
            ReadWriteNBT components = itemNbt.getCompound(KEY_OF_NBT);
            items.set(slot, new OPanelItemStack(
                slot,
                id,
                count,
                components == null ? null : components.toString(),
                "ender"
            ));
        }

        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) throws NbtApiException {
        try {
            ReadWriteNBTCompoundList list = nbt.getCompoundList("Inventory");
            if(list == null) return;

            for(int i = list.size() - 1; i >= 0; i--) {
                int slot = toPanelSlot(list.get(i).getByte("Slot"));
                if(isValidMainSlot(slot)) {
                    list.remove(i);
                }
            }

            for(OPanelItemStack item : items) {
                if(item == null) continue;
                if(!isValidMainSlot(item.slot)) continue;
                if(item == null || item.isEmpty()) continue;
                list.addCompound(toNbt(item));
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) throws NbtApiException {
        try {
            if(item == null || !isValidMainSlot(item.slot)) return;

            ReadWriteNBTCompoundList list = nbt.getCompoundList("Inventory");
            if(list == null) return;

            for(int i = list.size() - 1; i >= 0; i--) {
                int slot = toPanelSlot(list.get(i).getByte("Slot"));
                if(slot == item.slot) {
                    list.remove(i);
                }
            }

            if(!item.isEmpty()) {
                list.addCompound(toNbt(item));
            }

            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEnderItem(OPanelItemStack item) throws NbtApiException {
        try {
            if(item == null || item.slot < 0 || item.slot >= ENDER_SLOT_COUNT) return;

            ReadWriteNBTCompoundList list = nbt.getCompoundList("EnderItems");
            if(list == null) return;

            for(int i = list.size() - 1; i >= 0; i--) {
                int slot = normalizeRawSlot(list.get(i).getByte("Slot"));
                if(slot == item.slot) {
                    list.remove(i);
                }
            }

            if(!item.isEmpty()) {
                list.addCompound(toEnderNbt(item));
            }

            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canReadEnderChest() {
        return true;
    }

    @Override
    public boolean canWriteEnderChest() {
        return true;
    }

    protected ReadWriteNBT toNbt(OPanelItemStack item) throws NbtApiException {
        int slot = toRawSlot(item.slot);
        if(slot < 0) {
            throw new IllegalArgumentException("Unsupported inventory slot: " + item.slot);
        }

        ReadWriteNBT itemNbt = NBT.createNBTObject();
        itemNbt.setByte("Slot", (byte) slot);
        itemNbt.setString("id", item.id);
        itemNbt.setByte(KEY_OF_COUNT, (byte) item.count);
        if(item.snbt != null) {
            itemNbt.set(KEY_OF_NBT, NBT.parseNBT(item.snbt), NBTHandlers.STORE_READWRITE_TAG);
        }
        return itemNbt;
    }

    protected ReadWriteNBT toEnderNbt(OPanelItemStack item) throws NbtApiException {
        ReadWriteNBT itemNbt = NBT.createNBTObject();
        itemNbt.setByte("Slot", (byte) item.slot);
        itemNbt.setString("id", item.id);
        itemNbt.setByte(KEY_OF_COUNT, (byte) item.count);
        if(item.snbt != null) {
            itemNbt.set(KEY_OF_NBT, NBT.parseNBT(item.snbt), NBTHandlers.STORE_READWRITE_TAG);
        }
        return itemNbt;
    }
}
