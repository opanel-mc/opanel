package net.opanel.forge_helper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Abstraction interface for resolving version-specific item data in Forge.
 * Different Minecraft versions handle item components and inventory APIs differently:
 * - 1.20.4 and earlier: Uses traditional NBT compound tags, direct field access
 * - 1.20.5+: Uses DataComponentTypes, getter methods
 * - 1.21.5+: Changed armor/offhand accessor APIs
 * 
 * Each version-specific module (forge-1.21, forge-1.20, etc.) should provide
 * an implementation of this interface to handle the version-specific code.
 */
public interface ItemDataResolver {

    /**
     * Resolve extra/advanced data from an ItemStack that requires version-specific APIs.
     * This includes NBT data, DataComponents, or any other version-specific item metadata.
     * 
     * @param item The ItemStack to extract data from
     * @return A map containing version-specific data (nbt, components, etc.)
     */
    Map<String, Object> resolveExtraData(ItemStack item);

    /**
     * Serialize a single ItemStack to a Map with all item properties.
     * This method handles both common properties (type, amount) and 
     * version-specific properties (enchantments, lore, durability, etc.)
     * 
     * @param item The ItemStack to serialize
     * @return A map containing all item data, or null if item is empty
     */
    Map<String, Object> serializeItem(ItemStack item);

    /**
     * Get armor items from player.
     * Returns list in order: [helmet, chestplate, leggings, boots]
     * 
     * API differences:
     * - 1.19.4-1.21.3: inventory.armor.get(index)
     * - 1.21.5+: inventory.getArmor(index) or player method
     * 
     * @param player The server player
     * @param inventory The player's inventory
     * @return List of armor ItemStacks in order [head, chest, legs, feet]
     */
    List<ItemStack> getArmorItems(ServerPlayer player, Inventory inventory);

    /**
     * Get the offhand item from player.
     * 
     * API differences:
     * - 1.19.4-1.21.3: inventory.offhand.get(0)
     * - 1.21.5+: player.getOffhandItem()
     * 
     * @param player The server player
     * @param inventory The player's inventory
     * @return The offhand ItemStack
     */
    ItemStack getOffhandItem(ServerPlayer player, Inventory inventory);

    /**
     * Get the version identifier this resolver is designed for.
     * 
     * @return Version string like "1.21", "1.20.5", etc.
     */
    String getTargetVersion();
}

