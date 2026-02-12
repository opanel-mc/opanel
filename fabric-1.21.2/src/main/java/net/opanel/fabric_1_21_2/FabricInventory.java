package net.opanel.fabric_1_21_2;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.opanel.fabric_helper.BaseFabricInventory;

import java.util.ArrayList;
import java.util.List;

public class FabricInventory extends BaseFabricInventory {
    private final DynamicOps<NbtElement> serializationContext;

    public FabricInventory(ServerPlayerEntity player, MinecraftServer server) {
        super(player);

        serializationContext = server.getRegistryManager().getOps(NbtOps.INSTANCE);
    }

    @Override
    protected String itemToId(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    @Override
    protected Item idToItem(String id) {
        return Registries.ITEM.get(Identifier.of(id));
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
            DataResult<NbtElement> encodeResult = ItemStack.CODEC.encodeStart(serializationContext, stack);
            NbtCompound nbt = (NbtCompound) encodeResult.result().orElse(new NbtCompound());
            NbtCompound components = nbt.getCompound("components");
            items.add(new OPanelItemStack(
                    i,
                    id,
                    stack.getCount(),
                    components.isEmpty() ? null : components.toString()
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
        itemNbt.putByte("count", (byte) Math.max(1, item.count));
        if(item.snbt != null) {
            itemNbt.put("components", StringNbtReader.parse(item.snbt));
        }
        DataResult<ItemStack> parseResult = ItemStack.CODEC.parse(serializationContext, itemNbt);
        return parseResult.result().orElse(ItemStack.EMPTY);
    }
}
