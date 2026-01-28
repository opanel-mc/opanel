package net.opanel.bukkit_helper;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Utility class for serializing player inventory to JSON-compatible format.
 * The inventory is converted to a 2D matrix structure for frontend consumption.
 * 
 * This class uses ItemDataResolver for version-specific data extraction,
 * allowing the core serialization logic to remain unchanged across versions.
 */
public class InventorySerializer {

    private static ItemDataResolver resolver = null;

    /**
     * Inject the version-specific ItemDataResolver.
     * Must be called during plugin initialization before any serialization.
     * 
     * @param dataResolver The version-specific resolver implementation
     */
    public static void setResolver(ItemDataResolver dataResolver) {
        resolver = dataResolver;
    }

    /**
     * Get the current resolver (for testing/debugging).
     */
    public static ItemDataResolver getResolver() {
        return resolver;
    }

    /**
     * Serialize a single ItemStack to a Map.
     * Returns null for empty slots.
     */
    public static Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        
        // Basic properties (stable across all versions)
        result.put("type", item.getType().name());
        result.put("amount", item.getAmount());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Display name (stable API)
            if (meta.hasDisplayName()) {
                result.put("displayName", meta.getDisplayName());
            }

            // Lore (stable API)
            if (meta.hasLore()) {
                result.put("lore", meta.getLore());
            }

            // Durability (stable API since 1.13+)
            if (meta instanceof Damageable) {
                Damageable damageable = (Damageable) meta;
                int maxDurability = item.getType().getMaxDurability();
                int currentDurability = maxDurability - damageable.getDamage();
                result.put("durability", currentDurability);
                result.put("maxDurability", maxDurability);
            }

            // Enchantments (stable API)
            Map<Enchantment, Integer> enchants = meta.getEnchants();
            if (!enchants.isEmpty()) {
                Map<String, Integer> enchantMap = new HashMap<>();
                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    enchantMap.put(entry.getKey().getKey().getKey(), entry.getValue());
                }
                result.put("enchantments", enchantMap);
            }

            // Custom model data (stable API since 1.14+)
            if (meta.hasCustomModelData()) {
                result.put("customModelData", meta.getCustomModelData());
            }

            // Unbreakable (stable API)
            if (meta.isUnbreakable()) {
                result.put("unbreakable", true);
            }
        }

        // Version-specific extra data (NBT, DataComponents, etc.)
        // Delegated to the injected resolver
        if (resolver != null) {
            try {
                Map<String, Object> extraData = resolver.resolveExtraData(item);
                if (extraData != null && !extraData.isEmpty()) {
                    result.putAll(extraData);
                }
            } catch (Exception e) {
                // Silently ignore resolver errors to prevent crashes
            }
        }

        return result;
    }

    /**
     * Serialize a player's complete inventory to a 2D matrix structure.
     * 
     * Structure:
     * - hotbar: 9 slots (Slot 0-8)
     * - inventory: 3x9 matrix (Slot 9-35)
     * - armor: [helmet, chestplate, leggings, boots]
     * - offhand: [item]
     */
    public static Map<String, Object> serializeInventory(Player player) {
        if (player == null) {
            return null;
        }

        PlayerInventory inv = player.getInventory();
        Map<String, Object> result = new HashMap<>();

        // Hotbar (Slot 0-8)
        List<Map<String, Object>> hotbar = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            hotbar.add(serializeItem(inv.getItem(i)));
        }
        result.put("hotbar", hotbar);

        // Main inventory as 3x9 matrix (Slot 9-35)
        List<List<Map<String, Object>>> inventory = new ArrayList<>(3);
        for (int row = 0; row < 3; row++) {
            List<Map<String, Object>> rowItems = new ArrayList<>(9);
            for (int col = 0; col < 9; col++) {
                int slot = 9 + (row * 9) + col;
                rowItems.add(serializeItem(inv.getItem(slot)));
            }
            inventory.add(rowItems);
        }
        result.put("inventory", inventory);

        // Armor slots [helmet, chestplate, leggings, boots]
        List<Map<String, Object>> armor = new ArrayList<>(4);
        armor.add(serializeItem(inv.getHelmet()));
        armor.add(serializeItem(inv.getChestplate()));
        armor.add(serializeItem(inv.getLeggings()));
        armor.add(serializeItem(inv.getBoots()));
        result.put("armor", armor);

        // Offhand
        List<Map<String, Object>> offhand = new ArrayList<>(1);
        offhand.add(serializeItem(inv.getItemInOffHand()));
        result.put("offhand", offhand);

        return result;
    }

    /**
     * Generate a hash of the inventory for change detection.
     * Used by the scheduled sync task to avoid unnecessary updates.
     */
    public static String generateInventoryHash(Player player) {
        if (player == null) {
            return "";
        }

        PlayerInventory inv = player.getInventory();
        StringBuilder sb = new StringBuilder();

        // Hash all slots
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                sb.append(i).append(":").append(item.getType().name())
                  .append(":").append(item.getAmount());
                if (item.hasItemMeta() && item.getItemMeta() instanceof Damageable) {
                    sb.append(":").append(((Damageable) item.getItemMeta()).getDamage());
                }
                sb.append(";");
            }
        }

        // Hash armor
        ItemStack[] armorContents = inv.getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            ItemStack item = armorContents[i];
            if (item != null && !item.getType().isAir()) {
                sb.append("A").append(i).append(":").append(item.getType().name())
                  .append(":").append(item.getAmount()).append(";");
            }
        }

        // Hash offhand
        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            sb.append("O:").append(offhand.getType().name())
              .append(":").append(offhand.getAmount()).append(";");
        }

        return sb.toString();
    }
}
