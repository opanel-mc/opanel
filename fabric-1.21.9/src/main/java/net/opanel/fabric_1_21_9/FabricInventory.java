package net.opanel.fabric_1_21_9;

import com.mojang.serialization.DataResult;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.opanel.fabric_helper.BaseFabricInventory;

import java.util.ArrayList;
import java.util.List;

public class FabricInventory extends BaseFabricInventory {
    private final MinecraftServer server;

    public FabricInventory(ServerPlayerEntity player, MinecraftServer server) {
        super(player);

        this.server = server;
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
            RegistryWrapper.WrapperLookup wrapperLookup = server.getRegistryManager();
            DataResult<NbtElement> encodeResult = ItemStack.CODEC.encodeStart(wrapperLookup.getOps(NbtOps.INSTANCE), stack);
            NbtCompound nbt = (NbtCompound) encodeResult.result().orElse(new NbtCompound());
            NbtCompound components = nbt.getCompoundOrEmpty("components");
            items.add(new OPanelItemStack(
                    i,
                    id,
                    stack.getCount(),
                    components.isEmpty() ? null : components.toString()
            ));
        }
        return items;
    }
}
