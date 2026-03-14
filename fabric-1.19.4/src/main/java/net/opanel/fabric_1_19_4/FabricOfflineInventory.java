package net.opanel.fabric_1_19_4;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;
import net.opanel.fabric_helper.BaseFabricOfflineInventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricOfflineInventory extends BaseFabricOfflineInventory {
    private static final int MAIN_LAST_SLOT = 35;
    private static final int PLAYER_SLOT_COUNT = 41;
    private static final int ENDER_SLOT_COUNT = 27;
    private static final int ARMOR_RAW_MIN = 100;
    private static final int ARMOR_RAW_MAX = 103;
    private static final int OFFHAND_RAW_SLOT = 150;

    private NbtCompound nbt;
    private NbtList nbtList;
    private NbtList enderNbtList;

    public FabricOfflineInventory(Path playerDataPath) {
        super(playerDataPath);

        try {
            nbt = NbtIo.readCompressed(playerDataPath.toFile());
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
        NbtIo.writeCompressed(nbt, playerDataPath.toFile());
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

        for(int i = 0; i < nbtList.size(); i++) {
            NbtCompound itemNbt = nbtList.getCompound(i);
            int slot = toPanelSlot(itemNbt.getByte("Slot"));
            if(!isValidMainSlot(slot)) continue;

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte("Count");
            NbtCompound tag = itemNbt.getCompound("tag");
            items.set(slot, new OPanelItemStack(slot, id, count, tag.isEmpty() ? null : tag.toString()));
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

        for(int i = 0; i < enderNbtList.size(); i++) {
            NbtCompound itemNbt = enderNbtList.getCompound(i);
            int slot = normalizeRawSlot(itemNbt.getByte("Slot"));
            if(slot < 0 || slot >= ENDER_SLOT_COUNT) continue;

            String id = itemNbt.getString("id");
            int count = itemNbt.getByte("Count");
            NbtCompound tag = itemNbt.getCompound("tag");
            items.set(slot, new OPanelItemStack(slot, id, count, tag.isEmpty() ? null : tag.toString(), "ender"));
        }

        return items;
    }

    @Override
    public void setItems(List<OPanelItemStack> items) throws CommandSyntaxException {
        try {
            for(int i = nbtList.size() - 1; i >= 0; i--) {
                int slot = toPanelSlot(nbtList.getCompound(i).getByte("Slot"));
                if(isValidMainSlot(slot)) {
                    nbtList.remove(i);
                }
            }

            for(OPanelItemStack item : items) {
                if(item == null || !isValidMainSlot(item.slot) || item.isEmpty()) continue;
                nbtList.add(toNbt(item));
            }
            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setItem(OPanelItemStack item) throws CommandSyntaxException {
        try {
            if(item == null || !isValidMainSlot(item.slot)) return;

            for(int i = nbtList.size() - 1; i >= 0; i--) {
                int slot = toPanelSlot(nbtList.getCompound(i).getByte("Slot"));
                if(slot == item.slot) {
                    nbtList.remove(i);
                }
            }

            if(!item.isEmpty()) {
                nbtList.add(toNbt(item));
            }

            saveNbt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEnderItem(OPanelItemStack item) throws CommandSyntaxException {
        try {
            if(item == null || item.slot < 0 || item.slot >= ENDER_SLOT_COUNT) return;

            for(int i = enderNbtList.size() - 1; i >= 0; i--) {
                int slot = normalizeRawSlot(enderNbtList.getCompound(i).getByte("Slot"));
                if(slot == item.slot) {
                    enderNbtList.remove(i);
                }
            }

            if(!item.isEmpty()) {
                enderNbtList.add(toEnderNbt(item));
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

    @Override
    protected NbtCompound toNbt(OPanelItemStack item) throws CommandSyntaxException {
        int slot = toRawSlot(item.slot);
        if(slot < 0) {
            throw new IllegalArgumentException("Unsupported inventory slot: " + item.slot);
        }

        NbtCompound itemNbt = new NbtCompound();
        itemNbt.putByte("Slot", (byte) slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("Count", (byte) item.count);
        if(item.snbt != null) {
            itemNbt.put("tag", StringNbtReader.parse(item.snbt));
        }
        return itemNbt;
    }

    private NbtCompound toEnderNbt(OPanelItemStack item) throws CommandSyntaxException {
        NbtCompound itemNbt = new NbtCompound();
        itemNbt.putByte("Slot", (byte) item.slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("Count", (byte) item.count);
        if(item.snbt != null) {
            itemNbt.put("tag", StringNbtReader.parse(item.snbt));
        }
        return itemNbt;
    }
}
