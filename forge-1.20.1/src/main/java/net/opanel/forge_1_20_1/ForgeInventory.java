package net.opanel.forge_1_20_1;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.opanel.forge_helper.BaseForgeInventory;

import java.util.ArrayList;
import java.util.List;

public class ForgeInventory extends BaseForgeInventory {
    public ForgeInventory(ServerPlayer player) {
        super(player);
    }

    @Override
    protected String itemToId(Item item) {
        ResourceLocation identifier = ForgeRegistries.ITEMS.getKey(item);
        if(identifier == null) return null;
        return identifier.toString();
    }

    @Override
    protected Item idToItem(String id) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
    }

    @Override
    public List<OPanelItemStack> getItems() {
        Inventory inventory = player.getInventory();
        int size = getSize();
        List<OPanelItemStack> items = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if(stack.isEmpty()) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
                continue;
            }

            final String id = itemToId(stack.getItem());
            CompoundTag nbt = stack.getTag();
            items.add(new OPanelItemStack(
                    i,
                    id,
                    stack.getCount(),
                    nbt == null ? null : nbt.toString()
            ));
        }
        return items;
    }

    @Override
    protected ItemStack toItemStack(OPanelItemStack item) throws CommandSyntaxException {
        if(item == null || item.isEmpty()) return ItemStack.EMPTY;

        CompoundTag itemNbt = new CompoundTag();
        itemNbt.putByte("Slot", (byte) item.slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("Count", (byte) Math.max(1, item.count));
        if(item.snbt != null) {
            itemNbt.put("tag", TagParser.parseTag(item.snbt));
        }
        return ItemStack.of(itemNbt);
    }
}
