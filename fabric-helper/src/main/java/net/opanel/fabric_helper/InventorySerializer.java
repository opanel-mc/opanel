package net.opanel.fabric_helper;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.security.MessageDigest;
import java.util.*;

/**
 * Utility class for serializing Fabric player inventory to JSON-compatible format.
 * Uses ItemDataResolver for version-specific item serialization.
 */
public class InventorySerializer {

    private static ItemDataResolver resolver = null;

    public static void setResolver(ItemDataResolver dataResolver) {
        resolver = dataResolver;
    }

    public static ItemDataResolver getResolver() {
        return resolver;
    }

    public static Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        if (resolver != null) {
            return resolver.serializeItem(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("type", item.getItem().toString().toUpperCase());
        result.put("amount", item.getCount());
        return result;
    }

    /**
     * Serialize entire player inventory to the 2D matrix format.
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

        // Armor - use version-specific resolver
        List<Map<String, Object>> armor = new ArrayList<>();
        if (resolver != null) {
            List<ItemStack> armorItems = resolver.getArmorItems(inventory);
            for (ItemStack stack : armorItems) {
                armor.add(serializeItem(stack));
            }
        }
        result.put("armor", armor);

        // Offhand - use version-specific resolver
        List<Map<String, Object>> offhand = new ArrayList<>();
        if (resolver != null) {
            offhand.add(serializeItem(resolver.getOffhandItem(inventory)));
        }
        result.put("offhand", offhand);

        return result;
    }

    /**
     * Generate a hash of the inventory for change detection.
     */
    public static String generateInventoryHash(ServerPlayerEntity player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();
        PlayerInventory inventory = player.getInventory();

        // Hash main inventory (slots 0-35)
        for (int i = 0; i < 36; i++) {
            appendItemHash(sb, inventory.getStack(i));
        }

        // Hash armor using version-specific resolver
        if (resolver != null) {
            for (ItemStack stack : resolver.getArmorItems(inventory)) {
                appendItemHash(sb, stack);
            }
            // Hash offhand
            appendItemHash(sb, resolver.getOffhandItem(inventory));
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

