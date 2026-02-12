package net.opanel.neoforge_1_21_1;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.opanel.common.OPanelInventory;

import java.util.ArrayList;
import java.util.List;

public class NeoInventory implements OPanelInventory {
    private final ServerPlayer player;
    private final DynamicOps<Tag> serializationContext;

    public NeoInventory(ServerPlayer player, MinecraftServer server) {
        this.player = player;
        serializationContext = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }

    @Override
    public int getSize() {
        return player.getInventory().getContainerSize();
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

            final String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            DataResult<Tag> encodeResult = ItemStack.CODEC.encodeStart(serializationContext, stack);
            CompoundTag nbt = (CompoundTag) encodeResult.result().orElse(new CompoundTag());
            CompoundTag components = nbt.getCompound("components");
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
    public void setItems(List<OPanelItemStack> items) throws CommandSyntaxException {
        Inventory inventory = player.getInventory();
        inventory.clearContent();

        for(OPanelItemStack item : items) {
            inventory.setItem(item.slot, toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelItemStack item) throws CommandSyntaxException {
        player.getInventory().setItem(item.slot, toItemStack(item));
    }

    protected ItemStack toItemStack(OPanelItemStack item) throws CommandSyntaxException {
        if(item == null || item.isEmpty()) return ItemStack.EMPTY;

        CompoundTag itemNbt = new CompoundTag();
        itemNbt.putByte("Slot", (byte) item.slot);
        itemNbt.putString("id", item.id);
        itemNbt.putByte("count", (byte) Math.max(1, item.count));
        if(item.snbt != null) {
            itemNbt.put("components", TagParser.parseTag(item.snbt));
        }
        DataResult<ItemStack> parseResult = ItemStack.CODEC.parse(serializationContext, itemNbt);
        return parseResult.result().orElse(ItemStack.EMPTY);
    }
}
