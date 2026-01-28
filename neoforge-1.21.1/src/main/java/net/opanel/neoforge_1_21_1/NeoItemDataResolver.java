package net.opanel.neoforge_1_21_1;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Interface for resolving version-specific item data in NeoForge.
 * NeoForge 1.21.1 uses DataComponentTypes API (1.20.5+).
 */
public interface NeoItemDataResolver {

    /**
     * Resolve extra/advanced data from an ItemStack.
     */
    Map<String, Object> resolveExtraData(ItemStack item);

    /**
     * Serialize a single ItemStack to a Map with all item properties.
     */
    Map<String, Object> serializeItem(ItemStack item);

    /**
     * Get the version identifier this resolver is designed for.
     */
    String getTargetVersion();
}
