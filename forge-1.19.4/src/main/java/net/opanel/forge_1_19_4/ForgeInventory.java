package net.opanel.forge_1_19_4;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.opanel.forge_helper.BaseForgeInventory;

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
}
