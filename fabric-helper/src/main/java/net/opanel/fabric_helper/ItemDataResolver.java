package net.opanel.fabric_helper;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Abstraction interface for resolving version-specific item data in Fabric.
 * Different Minecraft versions handle item components differently:
 * - 1.20.4 and earlier: Uses traditional NBT compound tags
 * - 1.20.5+: Uses DataComponentTypes
 * 
 * Each version-specific module (fabric-1.21, fabric-1.20, etc.) should provide
 * an implementation of this interface to handle the version-specific code.
 */
public interface ItemDataResolver {

    /**
     * Resolve extra/advanced data from an ItemStack that requires version-specific APIs.
     */
    Map<String, Object> resolveExtraData(ItemStack item);

    /**
     * Serialize a single ItemStack to a Map with all item properties.
     */
    Map<String, Object> serializeItem(ItemStack item);

    /**
     * Serialize armor slots from player inventory.
     * Returns list in order: [helmet, chestplate, leggings, boots]
     */
    List<ItemStack> getArmorItems(PlayerInventory inventory);

    /**
     * Get the offhand item from player inventory.
     */
    ItemStack getOffhandItem(PlayerInventory inventory);

    /**
     * Get the version identifier this resolver is designed for.
     */
    String getTargetVersion();
}

