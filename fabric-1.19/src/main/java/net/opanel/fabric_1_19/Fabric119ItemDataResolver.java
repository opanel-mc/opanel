package net.opanel.fabric_1_19;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.opanel.fabric_helper.ItemDataResolver;

import java.util.*;

public class Fabric119ItemDataResolver implements ItemDataResolver {

    @Override
    public Map<String, Object> resolveExtraData(ItemStack item) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("type", item.getItem().toString().toUpperCase());
        result.put("amount", item.getCount());
        if (item.hasCustomName()) {
            Text customName = item.getName();
            if (customName != null) result.put("displayName", customName.getString());
        }
        NbtCompound nbt = item.getNbt();
        if (nbt != null) {
            if (nbt.contains("display")) {
                NbtCompound display = nbt.getCompound("display");
                if (display.contains("Lore")) {
                    List<String> loreList = new ArrayList<>();
                    for (int i = 0; i < display.getList("Lore", 8).size(); i++) {
                        loreList.add(display.getList("Lore", 8).getString(i));
                    }
                    if (!loreList.isEmpty()) result.put("lore", loreList);
                }
            }
            if (nbt.contains("Enchantments")) {
                Map<String, Integer> enchantMap = new HashMap<>();
                var enchantList = nbt.getList("Enchantments", 10);
                for (int i = 0; i < enchantList.size(); i++) {
                    NbtCompound enchant = enchantList.getCompound(i);
                    String id = enchant.getString("id");
                    int level = enchant.getShort("lvl");
                    if (id.contains(":")) id = id.split(":")[1];
                    enchantMap.put(id, level);
                }
                if (!enchantMap.isEmpty()) result.put("enchantments", enchantMap);
            }
            if (nbt.contains("CustomModelData")) result.put("customModelData", nbt.getInt("CustomModelData"));
            if (nbt.contains("Unbreakable") && nbt.getBoolean("Unbreakable")) result.put("unbreakable", true);
        }
        if (item.isDamageable()) {
            result.put("durability", item.getMaxDamage() - item.getDamage());
            result.put("maxDurability", item.getMaxDamage());
        }
        return result;
    }

    @Override
    public List<ItemStack> getArmorItems(PlayerInventory inventory) {
        List<ItemStack> armor = new ArrayList<>();
        armor.add(inventory.armor.get(3)); // Helmet
        armor.add(inventory.armor.get(2)); // Chestplate
        armor.add(inventory.armor.get(1)); // Leggings
        armor.add(inventory.armor.get(0)); // Boots
        return armor;
    }

    @Override
    public ItemStack getOffhandItem(PlayerInventory inventory) {
        return inventory.offHand.get(0);
    }

    @Override
    public String getTargetVersion() { return "1.19"; }
}
