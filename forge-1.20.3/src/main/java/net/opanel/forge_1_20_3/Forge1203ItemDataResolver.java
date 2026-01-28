package net.opanel.forge_1_20_3;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.opanel.forge_helper.ItemDataResolver;

import java.util.*;

/**
 * Forge 1.20.3 implementation of ItemDataResolver.
 * Uses traditional NBT-based item data (pre-1.20.5 DataComponents).
 */
public class Forge1203ItemDataResolver implements ItemDataResolver {

    @Override
    public Map<String, Object> resolveExtraData(ItemStack item) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("type", item.getItem().toString().toUpperCase());
        result.put("amount", item.getCount());

        if (item.hasCustomHoverName()) {
            Component customName = item.getHoverName();
            if (customName != null) result.put("displayName", customName.getString());
        }

        CompoundTag nbt = item.getTag();
        if (nbt != null) {
            if (nbt.contains("display")) {
                CompoundTag display = nbt.getCompound("display");
                if (display.contains("Lore")) {
                    ListTag loreTag = display.getList("Lore", 8);
                    List<String> loreList = new ArrayList<>();
                    for (int i = 0; i < loreTag.size(); i++) loreList.add(loreTag.getString(i));
                    if (!loreList.isEmpty()) result.put("lore", loreList);
                }
            }
            if (nbt.contains("Enchantments")) {
                ListTag enchantList = nbt.getList("Enchantments", 10);
                Map<String, Integer> enchantMap = new HashMap<>();
                for (int i = 0; i < enchantList.size(); i++) {
                    CompoundTag enchant = enchantList.getCompound(i);
                    String id = enchant.getString("id");
                    int level = enchant.getShort("lvl");
                    if (id.contains(":")) id = id.split(":")[1];
                    enchantMap.put(id, level);
                }
                if (!enchantMap.isEmpty()) result.put("enchantments", enchantMap);
            }
            if (nbt.contains("Unbreakable") && nbt.getBoolean("Unbreakable")) result.put("unbreakable", true);
        }

        if (item.isDamageableItem()) {
            result.put("durability", item.getMaxDamage() - item.getDamageValue());
            result.put("maxDurability", item.getMaxDamage());
        }

        return result;
    }

    @Override
    public List<ItemStack> getArmorItems(ServerPlayer player, Inventory inventory) {
        List<ItemStack> armor = new ArrayList<>();
        armor.add(inventory.armor.get(3));
        armor.add(inventory.armor.get(2));
        armor.add(inventory.armor.get(1));
        armor.add(inventory.armor.get(0));
        return armor;
    }

    @Override
    public ItemStack getOffhandItem(ServerPlayer player, Inventory inventory) {
        return inventory.offhand.get(0);
    }

    @Override
    public String getTargetVersion() { return "1.20.3"; }
}
