package net.opanel.forge_1_21_5;

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
 * Forge 1.21.5 implementation of ItemDataResolver.
 * Uses Minecraft 1.20.5+ DataComponents API.
 * 
 * IMPORTANT - Inventory API changed in 1.21.5:
 * - Armor: Use getArmor(index) method instead of armor field
 * - Offhand: Use player.getOffhandItem() instead of offhand field
 * - inventory.armor / inventory.offhand fields are now private
 */
public class Forge1215ItemDataResolver implements ItemDataResolver {

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
        // 1.21.5+: Use getArmor(index) method - index 3=head, 2=chest, 1=legs, 0=feet
        List<ItemStack> armor = new ArrayList<>();
        armor.add(inventory.getArmor(3)); // Helmet
        armor.add(inventory.getArmor(2)); // Chestplate
        armor.add(inventory.getArmor(1)); // Leggings
        armor.add(inventory.getArmor(0)); // Boots
        return armor;
    }

    @Override
    public ItemStack getOffhandItem(ServerPlayer player, Inventory inventory) {
        // 1.21.5+: Use player.getOffhandItem() method
        return player.getOffhandItem();
    }

    @Override
    public String getTargetVersion() {
        return "1.21.5";
    }
}
