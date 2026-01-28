package net.opanel.spigot_1_21;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import net.opanel.bukkit_helper.ItemDataResolver;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Spigot 1.21 implementation of ItemDataResolver.
 * 
 * This version uses the NBT-API for extracting NBT data.
 * In 1.20.5+ Minecraft moved to DataComponents, but NBT-API
 * abstracts this difference for us.
 */
public class Spigot121ItemDataResolver implements ItemDataResolver {

    @Override
    public Map<String, Object> resolveExtraData(ItemStack item) {
        Map<String, Object> result = new HashMap<>();
        
        if (item == null || item.getType().isAir()) {
            return result;
        }

        // Use NBT-API which handles the 1.20.5+ DataComponent transition
        try {
            ReadableNBT nbt = NBT.itemStackToNBT(item);
            if (nbt != null) {
                result.put("nbt", nbt.toString());
            }
        } catch (Exception e) {
            // Silently ignore NBT serialization errors
        }

        return result;
    }

    @Override
    public String getTargetVersion() {
        return "1.21";
    }

    @Override
    public boolean isCompatible() {
        // Check if we're running on a compatible server
        try {
            Class.forName("org.bukkit.inventory.ItemStack");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
