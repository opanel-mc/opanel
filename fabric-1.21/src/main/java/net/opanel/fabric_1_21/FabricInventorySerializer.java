package net.opanel.fabric_1_21;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.security.MessageDigest;
import java.util.*;

/**
 * Utility class for serializing Fabric player inventories to a JSON-compatible format
 */
public class FabricInventorySerializer {

    /**
     * Serialize a single ItemStack to a Map
     */
    public static Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();

        // Basic properties
        result.put("type", item.getItem().toString().toUpperCase());
        result.put("amount", item.getCount());

        // Display name
        if (item.contains(DataComponentTypes.CUSTOM_NAME)) {
            Text customName = item.get(DataComponentTypes.CUSTOM_NAME);
            if (customName != null) {
                result.put("displayName", customName.getString());
            }
        }

        // Lore
        if (item.contains(DataComponentTypes.LORE)) {
            LoreComponent loreComponent = item.get(DataComponentTypes.LORE);
            if (loreComponent != null) {
                List<String> loreList = new ArrayList<>();
                for (Text line : loreComponent.lines()) {
                    loreList.add(line.getString());
                }
                if (!loreList.isEmpty()) {
                    result.put("lore", loreList);
                }
            }
        }

        // Durability
        if (item.contains(DataComponentTypes.DAMAGE) && item.contains(DataComponentTypes.MAX_DAMAGE)) {
            Integer damage = item.get(DataComponentTypes.DAMAGE);
            Integer maxDamage = item.get(DataComponentTypes.MAX_DAMAGE);
            if (damage != null && maxDamage != null) {
                result.put("durability", maxDamage - damage);
                result.put("maxDurability", maxDamage);
            }
        }

        // Enchantments
        if (item.contains(DataComponentTypes.ENCHANTMENTS)) {
            ItemEnchantmentsComponent enchantments = item.get(DataComponentTypes.ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                Map<String, Integer> enchantMap = new HashMap<>();
                for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
                    Optional<RegistryKey<Enchantment>> key = entry.getKey();
                    if (key.isPresent()) {
                        enchantMap.put(key.get().getValue().getPath(), enchantments.getLevel(entry));
                    }
                }
                if (!enchantMap.isEmpty()) {
                    result.put("enchantments", enchantMap);
                }
            }
        }

        // Custom model data
        if (item.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            result.put("customModelData", item.get(DataComponentTypes.CUSTOM_MODEL_DATA));
        }

        // Unbreakable
        if (item.contains(DataComponentTypes.UNBREAKABLE)) {
            result.put("unbreakable", true);
        }

        return result;
    }

    /**
     * Serialize entire player inventory to the 2D matrix format
     */
    public static Map<String, Object> serializeInventory(ServerPlayerEntity player) {
        if (player == null) return null;

        PlayerInventory inventory = player.getInventory();
        Map<String, Object> result = new HashMap<>();

        // Hotbar (slots 0-8)
        List<Map<String, Object>> hotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            hotbar.add(serializeItem(inventory.getStack(i)));
        }
        result.put("hotbar", hotbar);

        // Main inventory (3x9 matrix, slots 9-35)
        List<List<Map<String, Object>>> mainInventory = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            List<Map<String, Object>> rowItems = new ArrayList<>();
            for (int col = 0; col < 9; col++) {
                int slot = 9 + (row * 9) + col;
                rowItems.add(serializeItem(inventory.getStack(slot)));
            }
            mainInventory.add(rowItems);
        }
        result.put("inventory", mainInventory);

        // Armor (helmet, chestplate, leggings, boots - in that order)
        List<Map<String, Object>> armor = new ArrayList<>();
        // Fabric uses index 3=head, 2=chest, 1=legs, 0=feet
        armor.add(serializeItem(inventory.armor.get(3))); // Helmet
        armor.add(serializeItem(inventory.armor.get(2))); // Chestplate
        armor.add(serializeItem(inventory.armor.get(1))); // Leggings
        armor.add(serializeItem(inventory.armor.get(0))); // Boots
        result.put("armor", armor);

        // Offhand
        List<Map<String, Object>> offhand = new ArrayList<>();
        offhand.add(serializeItem(inventory.offHand.get(0)));
        result.put("offhand", offhand);

        return result;
    }

    /**
     * Generate a hash of the inventory for change detection
     */
    public static String generateInventoryHash(ServerPlayerEntity player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();
        PlayerInventory inventory = player.getInventory();

        // Hash main inventory
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            appendItemHash(sb, stack);
        }

        // Hash armor
        for (ItemStack stack : inventory.armor) {
            appendItemHash(sb, stack);
        }

        // Hash offhand
        for (ItemStack stack : inventory.offHand) {
            appendItemHash(sb, stack);
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return sb.toString();
        }
    }

    private static void appendItemHash(StringBuilder sb, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            sb.append("null|");
        } else {
            sb.append(stack.getItem().toString())
              .append(":")
              .append(stack.getCount());

            if (stack.contains(DataComponentTypes.DAMAGE)) {
                sb.append(":").append(stack.get(DataComponentTypes.DAMAGE));
            }

            sb.append("|");
        }
    }
}
