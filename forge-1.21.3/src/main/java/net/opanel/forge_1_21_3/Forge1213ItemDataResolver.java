package net.opanel.forge_1_21_3;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.opanel.forge_helper.ItemDataResolver;

import java.util.*;

/**
 * Forge 1.21.3 implementation of ItemDataResolver.
 * Uses Minecraft 1.20.5+ DataComponents API.
 * 
 * Inventory API (1.21.3):
 * - Armor: inventory.armor.get(index)
 * - Offhand: inventory.offhand.get(0)
 */
public class Forge1213ItemDataResolver implements ItemDataResolver {

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

        Component customName = item.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            result.put("displayName", customName.getString());
        }

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

        Integer damage = item.get(DataComponents.DAMAGE);
        Integer maxDamage = item.get(DataComponents.MAX_DAMAGE);
        if (damage != null && maxDamage != null) {
            result.put("durability", maxDamage - damage);
            result.put("maxDurability", maxDamage);
        }

        ItemEnchantments enchantments = item.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            Map<String, Integer> enchantMap = new HashMap<>();
            for (Holder<Enchantment> entry : enchantments.keySet()) {
                entry.unwrapKey().ifPresent(key -> {
                    // Use entry.value() to get Enchantment from Holder
                    enchantMap.put(key.location().getPath(), enchantments.getLevel(entry.value()));
                });
            }
            if (!enchantMap.isEmpty()) {
                result.put("enchantments", enchantMap);
            }
        }

        if (item.has(DataComponents.UNBREAKABLE)) {
            result.put("unbreakable", true);
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
    public String getTargetVersion() {
        return "1.21.3";
    }
}
