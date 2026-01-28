package net.opanel.forge_1_19_4;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.opanel.forge_helper.ItemDataResolver;

import java.util.*;

/**
 * Forge 1.19.4 implementation of ItemDataResolver.
 * Uses traditional NBT-based item data (pre-1.20.5 DataComponents).
 * 
 * Inventory API (1.19.4):
 * - Armor: inventory.armor.get(index) - index 3=head, 2=chest, 1=legs, 0=feet
 * - Offhand: inventory.offhand.get(0)
 * - Main: inventory.items.get(index) or inventory.getItem(index)
 */
public class Forge1194ItemDataResolver implements ItemDataResolver {

    @Override
    public Map<String, Object> resolveExtraData(ItemStack item) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();

        // Basic properties
        result.put("type", item.getItem().toString().toUpperCase());
        result.put("amount", item.getCount());

        // Display name
        if (item.hasCustomHoverName()) {
            Component customName = item.getHoverName();
            if (customName != null) {
                result.put("displayName", customName.getString());
            }
        }

        // NBT-based data (pre-1.20.5)
        CompoundTag nbt = item.getTag();
        if (nbt != null) {
            // Lore
            if (nbt.contains("display")) {
                CompoundTag display = nbt.getCompound("display");
                if (display.contains("Lore")) {
                    ListTag loreTag = display.getList("Lore", 8); // 8 = String tag type
                    List<String> loreList = new ArrayList<>();
                    for (int i = 0; i < loreTag.size(); i++) {
                        loreList.add(loreTag.getString(i));
                    }
                    if (!loreList.isEmpty()) {
                        result.put("lore", loreList);
                    }
                }
            }

            // Enchantments
            if (nbt.contains("Enchantments")) {
                ListTag enchantList = nbt.getList("Enchantments", 10); // 10 = Compound tag type
                Map<String, Integer> enchantMap = new HashMap<>();
                for (int i = 0; i < enchantList.size(); i++) {
                    CompoundTag enchant = enchantList.getCompound(i);
                    String id = enchant.getString("id");
                    int level = enchant.getShort("lvl");
                    if (id.contains(":")) {
                        id = id.split(":")[1];
                    }
                    enchantMap.put(id, level);
                }
                if (!enchantMap.isEmpty()) {
                    result.put("enchantments", enchantMap);
                }
            }

            // Unbreakable
            if (nbt.contains("Unbreakable") && nbt.getBoolean("Unbreakable")) {
                result.put("unbreakable", true);
            }
        }

        // Durability
        if (item.isDamageableItem()) {
            result.put("durability", item.getMaxDamage() - item.getDamageValue());
            result.put("maxDurability", item.getMaxDamage());
        }

        return result;
    }

    @Override
    public List<ItemStack> getArmorItems(ServerPlayer player, Inventory inventory) {
        // 1.19.4: Uses inventory.armor list - index 3=head, 2=chest, 1=legs, 0=feet
        List<ItemStack> armor = new ArrayList<>();
        armor.add(inventory.armor.get(3)); // Helmet
        armor.add(inventory.armor.get(2)); // Chestplate
        armor.add(inventory.armor.get(1)); // Leggings
        armor.add(inventory.armor.get(0)); // Boots
        return armor;
    }

    @Override
    public ItemStack getOffhandItem(ServerPlayer player, Inventory inventory) {
        // 1.19.4: Uses inventory.offhand list
        return inventory.offhand.get(0);
    }

    @Override
    public String getTargetVersion() {
        return "1.19.4";
    }
}
