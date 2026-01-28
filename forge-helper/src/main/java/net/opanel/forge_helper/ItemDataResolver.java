package net.opanel.forge_helper;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Abstraction interface for resolving version-specific item data in Forge.
 * Different Minecraft versions handle item components differently:
 * - 1.20.4 and earlier: Uses traditional NBT compound tags
 * - 1.20.5+: Uses DataComponentTypes
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
     * Get the version identifier this resolver is designed for.
     * 
     * @return Version string like "1.21", "1.20.5", etc.
     */
    String getTargetVersion();
}
