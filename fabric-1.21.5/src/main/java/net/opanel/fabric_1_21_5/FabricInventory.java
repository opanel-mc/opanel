package net.opanel.fabric_1_21_5;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.opanel.fabric_helper.BaseFabricInventory;

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
        return Registries.ITEM.get(Identifier.of(id));
    }
}
