package net.opanel.bukkit_helper;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Abstraction interface for resolving version-specific item data.
 * Different Minecraft versions handle NBT/DataComponents differently:
 * - 1.12-1.20.4: Uses traditional NBT compound tags
 * - 1.20.5+: Uses DataComponentTypes
 * 
 * Each version-specific module (spigot-1.21, fabric-1.20, etc.) should provide
 * an implementation of this interface to handle the 5% of version-specific code.
 */
public interface ItemDataResolver {

    /**
     * Resolve extra/advanced data from an ItemStack that requires version-specific APIs.
     * This includes NBT data, DataComponents, or any other version-specific item metadata.
     * 
     * @param item The ItemStack to extract data from
     * @return A map containing version-specific data (nbt, components, etc.)
     *         Keys may include: "nbt", "components", "customData", etc.
     *         Returns empty map if no extra data available.
     */
    Map<String, Object> resolveExtraData(ItemStack item);

    /**
     * Check if this resolver supports the current server version.
     * Used for runtime validation.
     * 
     * @return true if compatible with current server
     */
    default boolean isCompatible() {
        return true;
    }

    /**
     * Get the version identifier this resolver is designed for.
     * 
     * @return Version string like "1.21", "1.20.5", etc.
     */
    String getTargetVersion();
}
