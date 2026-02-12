package net.opanel.fabric_1_20_2;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.opanel.fabric_helper.BaseFabricInventory;

import java.util.ArrayList;
import java.util.List;

public class FabricInventory extends BaseFabricInventory {
    public FabricInventory(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    protected String itemToId(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    @Override
    protected Item idToItem(String id) {
        return Registries.ITEM.get(new Identifier(id));
    }

    @Override
    public List<OPanelItemStack> getItems() {
        PlayerInventory inventory = player.getInventory();
        int size = getSize();
        List<OPanelItemStack> items = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            ItemStack stack = inventory.getStack(i);
            if(stack.isEmpty()) {
                items.add(new OPanelItemStack(i, "minecraft:air", 0, null));
                continue;
            }

            final String id = itemToId(stack.getItem());
            NbtCompound nbt = stack.getNbt();
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

        NbtCompound itemNbt = new NbtCompound();
        itemNbt.putByte("Slot", (byte) item.slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("Count", (byte) Math.max(1, item.count));
        if(item.snbt != null) {
            itemNbt.put("tag", StringNbtReader.parse(item.snbt));
        }
        return ItemStack.fromNbt(itemNbt);
    }
}
