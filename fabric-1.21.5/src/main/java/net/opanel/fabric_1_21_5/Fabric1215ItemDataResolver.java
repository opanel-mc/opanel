package net.opanel.fabric_1_21_5;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.opanel.fabric_helper.ItemDataResolver;

import java.util.*;

public class Fabric1215ItemDataResolver implements ItemDataResolver {
    @Override
    public Map<String, Object> resolveExtraData(ItemStack item) { return new HashMap<>(); }

    @Override
    public Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("type", item.getItem().toString().toUpperCase());
        result.put("amount", item.getCount());
        if (item.contains(DataComponentTypes.CUSTOM_NAME)) {
            Text customName = item.get(DataComponentTypes.CUSTOM_NAME);
            if (customName != null) result.put("displayName", customName.getString());
        }
        if (item.contains(DataComponentTypes.LORE)) {
            LoreComponent loreComponent = item.get(DataComponentTypes.LORE);
            if (loreComponent != null) {
                List<String> loreList = new ArrayList<>();
                for (Text line : loreComponent.lines()) loreList.add(line.getString());
                if (!loreList.isEmpty()) result.put("lore", loreList);
            }
        }
        if (item.contains(DataComponentTypes.DAMAGE) && item.contains(DataComponentTypes.MAX_DAMAGE)) {
            Integer damage = item.get(DataComponentTypes.DAMAGE);
            Integer maxDamage = item.get(DataComponentTypes.MAX_DAMAGE);
            if (damage != null && maxDamage != null) {
                result.put("durability", maxDamage - damage);
                result.put("maxDurability", maxDamage);
            }
        }
        if (item.contains(DataComponentTypes.ENCHANTMENTS)) {
            ItemEnchantmentsComponent enchantments = item.get(DataComponentTypes.ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                Map<String, Integer> enchantMap = new HashMap<>();
                for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
                    Optional<RegistryKey<Enchantment>> key = entry.getKey();
                    if (key.isPresent()) enchantMap.put(key.get().getValue().getPath(), enchantments.getLevel(entry));
                }
                if (!enchantMap.isEmpty()) result.put("enchantments", enchantMap);
            }
        }
        if (item.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) result.put("customModelData", item.get(DataComponentTypes.CUSTOM_MODEL_DATA));
        if (item.contains(DataComponentTypes.UNBREAKABLE)) result.put("unbreakable", true);
        return result;
    }

    @Override
    public List<ItemStack> getArmorItems(PlayerInventory inventory) {
        // Fabric 1.21.5+: Use getStack with slot indices (36-39 for armor, 40 for offhand)
        List<ItemStack> armor = new ArrayList<>();
        armor.add(inventory.getStack(39)); // Helmet
        armor.add(inventory.getStack(38)); // Chestplate
        armor.add(inventory.getStack(37)); // Leggings
        armor.add(inventory.getStack(36)); // Boots
        return armor;
    }

    @Override
    public ItemStack getOffhandItem(PlayerInventory inventory) {
        return inventory.getStack(40);
    }

    @Override
    public String getTargetVersion() { return "1.21.5"; }
}
