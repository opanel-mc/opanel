package net.opanel.forge_helper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.security.MessageDigest;
import java.util.*;

/**
 * Utility class for serializing Forge player inventory to JSON-compatible format.
 * Uses ItemDataResolver for version-specific item serialization.
 */
public class InventorySerializer {

    private static ItemDataResolver resolver = null;

    /**
     * Inject the version-specific ItemDataResolver.
     * Must be called during mod initialization before any serialization.
     */
    public static void setResolver(ItemDataResolver dataResolver) {
        resolver = dataResolver;
    }

    /**
     * Get the current resolver.
     */
    public static ItemDataResolver getResolver() {
        return resolver;
    }

    /**
     * Serialize a single ItemStack to a Map.
     * Delegates to the injected resolver for version-specific handling.
     */
    public static Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        if (resolver != null) {
            return resolver.serializeItem(item);
        }

        // Fallback: basic serialization without resolver
        Map<String, Object> result = new HashMap<>();
        result.put("type", item.getItem().toString().toUpperCase());
        result.put("amount", item.getCount());
        return result;
    }

    /**
     * Serialize entire player inventory to the 2D matrix format.
     * 
     * Structure:
     * - hotbar: 9 slots (Slot 0-8)
     * - inventory: 3x9 matrix (Slot 9-35)
     * - armor: [helmet, chestplate, leggings, boots]
     * - offhand: [item]
     */
    public static Map<String, Object> serializeInventory(ServerPlayer player) {
        if (player == null) return null;

        Inventory inventory = player.getInventory();
        Map<String, Object> result = new HashMap<>();

        // Hotbar (slots 0-8)
        List<Map<String, Object>> hotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            hotbar.add(serializeItem(inventory.getItem(i)));
        }
        result.put("hotbar", hotbar);

        // Main inventory (3x9 matrix, slots 9-35)
        List<List<Map<String, Object>>> mainInventory = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            List<Map<String, Object>> rowItems = new ArrayList<>();
            for (int col = 0; col < 9; col++) {
                int slot = 9 + (row * 9) + col;
                rowItems.add(serializeItem(inventory.getItem(slot)));
            }
            mainInventory.add(rowItems);
        }
        result.put("inventory", mainInventory);

        // Armor (helmet, chestplate, leggings, boots)
        // Forge uses index 3=head, 2=chest, 1=legs, 0=feet
        List<Map<String, Object>> armor = new ArrayList<>();
        armor.add(serializeItem(inventory.armor.get(3))); // Helmet
        armor.add(serializeItem(inventory.armor.get(2))); // Chestplate
        armor.add(serializeItem(inventory.armor.get(1))); // Leggings
        armor.add(serializeItem(inventory.armor.get(0))); // Boots
        result.put("armor", armor);

        // Offhand
        List<Map<String, Object>> offhand = new ArrayList<>();
        offhand.add(serializeItem(inventory.offhand.get(0)));
        result.put("offhand", offhand);

        return result;
    }

    /**
     * Generate a hash of the inventory for change detection.
     */
    public static String generateInventoryHash(ServerPlayer player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();
        Inventory inventory = player.getInventory();

        // Hash main inventory (items list)
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            appendItemHash(sb, stack);
        }

        // Hash armor
        for (ItemStack stack : inventory.armor) {
            appendItemHash(sb, stack);
        }

        // Hash offhand
        for (ItemStack stack : inventory.offhand) {
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
              .append(stack.getCount())
              .append("|");
        }
    }
}
