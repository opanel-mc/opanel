package net.opanel.forge_1_21;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.network.chat.Component;
import net.opanel.forge_helper.ItemDataResolver;

import java.util.*;

/**
 * Forge 1.21 implementation of ItemDataResolver.
 * Uses Minecraft 1.20.5+ DataComponents API.
 * 
 * For 1.21 (before 1.21.5), armor/offhand are accessed via:
 * - inventory.armor.get(index)
 * - inventory.offhand.get(0)
 */
public class Forge121ItemDataResolver implements ItemDataResolver {

    @Override
    public Map<String, Object> resolveExtraData(ItemStack item) {
        // For Forge 1.21, extra data is already handled in serializeItem
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

        // Display name (DataComponents API)
        Component customName = item.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            result.put("displayName", customName.getString());
        }

        // Lore (DataComponents API)
        var loreComponent = item.get(DataComponents.LORE);
        if (loreComponent != null) {
            List<String> loreList = new ArrayList<>();
            for (Component line : loreComponent.lines()) {
                loreList.add(line.getString());
            }
            if (!loreList.isEmpty()) {
                result.put("lore", loreList);
            }
        }

        // Durability (DataComponents API)
        Integer damage = item.get(DataComponents.DAMAGE);
        Integer maxDamage = item.get(DataComponents.MAX_DAMAGE);
        if (damage != null && maxDamage != null) {
            result.put("durability", maxDamage - damage);
            result.put("maxDurability", maxDamage);
        }

        // Enchantments (DataComponents API)
        ItemEnchantments enchantments = item.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            Map<String, Integer> enchantMap = new HashMap<>();
            for (Holder<Enchantment> entry : enchantments.keySet()) {
                entry.unwrapKey().ifPresent(key -> {
                    enchantMap.put(key.location().getPath(), enchantments.getLevel(entry.value()));
                });
            }
            if (!enchantMap.isEmpty()) {
                result.put("enchantments", enchantMap);
            }
        }

        // Unbreakable (DataComponents API)
        if (item.has(DataComponents.UNBREAKABLE)) {
            result.put("unbreakable", true);
        }

        return result;
    }

    /**
     * Get armor items in order: [helmet, chestplate, leggings, boots]
     * For 1.21: Uses inventory.armor list (index 3=head, 2=chest, 1=legs, 0=feet)
     */
    @Override
    public List<ItemStack> getArmorItems(ServerPlayer player, Inventory inventory) {
        List<ItemStack> armor = new ArrayList<>();
        armor.add(inventory.armor.get(3)); // Helmet
        armor.add(inventory.armor.get(2)); // Chestplate
        armor.add(inventory.armor.get(1)); // Leggings
        armor.add(inventory.armor.get(0)); // Boots
        return armor;
    }

    /**
     * Get offhand item.
     * For 1.21: Uses inventory.offhand list
     */
    @Override
    public ItemStack getOffhandItem(ServerPlayer player, Inventory inventory) {
        return inventory.offhand.get(0);
    }

    @Override
    public String getTargetVersion() {
        return "1.21";
    }
}

