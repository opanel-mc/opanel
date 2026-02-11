package net.opanel.neoforge_1_21_1;

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
import net.opanel.common.OPanelInventory;

import java.util.ArrayList;
import java.util.List;

public class NeoInventory implements OPanelInventory {
    private final ServerPlayer player;
    private final MinecraftServer server;

    public NeoInventory(ServerPlayer player, MinecraftServer server) {
        this.player = player;
        this.server = server;
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
            RegistryAccess.Frozen registryAccess = server.registryAccess();
            DataResult<Tag> encodeResult = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack);
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
    public void setItems(List<OPanelItemStack> items) {
        Inventory inventory = player.getInventory();
        inventory.clearContent();

        for(OPanelItemStack item : items) {
            inventory.setItem(item.slot, toItemStack(item));
        }
    }

    @Override
    public void setItem(OPanelItemStack item) {
        player.getInventory().setItem(item.slot, toItemStack(item));
    }

    protected ItemStack toItemStack(OPanelItemStack item) {
        if(item == null || item.isEmpty()) return ItemStack.EMPTY;
        Item mcItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(item.id));
        return new ItemStack(mcItem, Math.max(1, item.count));
    }
}
