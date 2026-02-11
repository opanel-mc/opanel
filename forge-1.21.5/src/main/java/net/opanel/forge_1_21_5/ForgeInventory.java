package net.opanel.forge_1_21_5;

import com.mojang.serialization.DataResult;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.opanel.forge_helper.BaseForgeInventory;

import java.util.ArrayList;
import java.util.List;

public class ForgeInventory extends BaseForgeInventory {
    private final MinecraftServer server;

    public ForgeInventory(ServerPlayer player, MinecraftServer server) {
        super(player);

        this.server = server;
    }

    @Override
    protected String itemToId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    protected Item idToItem(String id) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.tryParse(id));
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
            RegistryAccess.Frozen registryAccess = server.registryAccess();
            DataResult<Tag> encodeResult = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack);
            CompoundTag nbt = (CompoundTag) encodeResult.result().orElse(new CompoundTag());
            CompoundTag components = nbt.getCompoundOrEmpty("components");
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
