package net.opanel.spigot_1_19_4;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import net.opanel.bukkit_helper.ItemDataResolver;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Spigot 1.19.4 implementation of ItemDataResolver.
 * Uses NBT-API for extracting NBT data from items.
 */
public class Spigot1194ItemDataResolver implements ItemDataResolver {

    @Override
    public Map<String, Object> resolveExtraData(ItemStack item) {
        Map<String, Object> result = new HashMap<>();
        
        if (item == null || item.getType().isAir()) {
            return result;
        }

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
        return "1.19.4";
    }

    @Override
    public boolean isCompatible() {
        try {
            Class.forName("org.bukkit.inventory.ItemStack");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
