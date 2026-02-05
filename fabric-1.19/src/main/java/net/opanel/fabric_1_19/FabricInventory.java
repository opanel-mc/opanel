package net.opanel.fabric_1_19;

import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.opanel.fabric_helper.BaseFabricInventory;

public class FabricInventory extends BaseFabricInventory {
    public FabricInventory(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    protected String itemToId(Item item) {
        return Registry.ITEM.getId(item).toString();
    }

    @Override
    protected Item idToItem(String id) {
        return Registry.ITEM.get(new Identifier(id));
    }
}
