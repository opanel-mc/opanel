package net.opanel.forge_1_21_8;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.opanel.forge_helper.BaseForgeInventory;

public class ForgeInventory extends BaseForgeInventory {
    public ForgeInventory(ServerPlayer player) {
        super(player);
    }

    @Override
    protected String itemToId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    protected Item idToItem(String id) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.tryParse(id));
    }
}
