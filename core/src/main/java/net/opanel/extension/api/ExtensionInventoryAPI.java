package net.opanel.extension.api;

import cn.opanel.api.player.InventoryAPI;
import cn.opanel.api.player.InventoryItem;
import cn.opanel.api.player.InventoryType;
import net.opanel.common.OPanelInventory;
import net.opanel.common.OPanelInventoryType;
import net.opanel.extension.ExtensionContext;

import java.util.*;

public final class ExtensionInventoryAPI implements InventoryAPI {
    private final ExtensionContext ctx;
    private final UUID uuid;

    ExtensionInventoryAPI(ExtensionContext ctx, UUID uuid) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    @Override
    public int getSize(InventoryType inventoryType) {
        Objects.requireNonNull(inventoryType, "inventoryType");
        return ctx.call("get inventory size", () -> inventory().getSize(toCommonType(inventoryType)));
    }

    @Override
    public List<InventoryItem> getItems(InventoryType inventoryType) {
        Objects.requireNonNull(inventoryType, "inventoryType");
        return ctx.call("get inventory items", () -> {
            List<InventoryItem> items = new ArrayList<>();
            for(OPanelInventory.OPanelItemStack item : inventory().getItems(toCommonType(inventoryType))) {
                items.add(toPublicItem(item));
            }
            return Collections.unmodifiableList(items);
        });
    }

    @Override
    public InventoryItem getItem(InventoryType inventoryType, int slot) {
        Objects.requireNonNull(inventoryType, "inventoryType");
        return ctx.call("get inventory item", () -> {
            OPanelInventory inventory = inventory();
            OPanelInventoryType commonType = toCommonType(inventoryType);
            validateSlot(inventory, commonType, slot);
            return toPublicItem(inventory.getItem(commonType, slot));
        });
    }

    @Override
    public void setItems(InventoryType inventoryType, List<InventoryItem> items) {
        Objects.requireNonNull(inventoryType, "inventoryType");
        Objects.requireNonNull(items, "items");
        List<InventoryItem> itemSnapshot = new ArrayList<>(items);

        ctx.run("set inventory items", () -> {
            OPanelInventory inventory = inventory();
            OPanelInventoryType commonType = toCommonType(inventoryType);
            Set<Integer> slots = new HashSet<>();
            List<OPanelInventory.OPanelItemStack> commonItems = new ArrayList<>(itemSnapshot.size());
            for(InventoryItem item : itemSnapshot) {
                Objects.requireNonNull(item, "item");
                validateSlot(inventory, commonType, item.getSlot());
                if(!slots.add(item.getSlot())) {
                    throw new IllegalArgumentException("Duplicate inventory slot: " + item.getSlot());
                }
                commonItems.add(toCommonItem(item));
            }
            inventory.setItems(commonType, commonItems);
        });
    }

    @Override
    public void setItem(InventoryType inventoryType, InventoryItem item) {
        Objects.requireNonNull(inventoryType, "inventoryType");
        Objects.requireNonNull(item, "item");
        ctx.run("set inventory item", () -> {
            OPanelInventory inventory = inventory();
            OPanelInventoryType commonType = toCommonType(inventoryType);
            validateSlot(inventory, commonType, item.getSlot());
            inventory.setItem(commonType, toCommonItem(item));
        });
    }

    private OPanelInventory inventory() {
        return ctx.getPlayer(uuid).getInventory();
    }

    private static void validateSlot(OPanelInventory inventory, OPanelInventoryType inventoryType, int slot) {
        if(slot < 0 || slot >= inventory.getSize(inventoryType)) {
            throw new IllegalArgumentException("Inventory slot is out of range: " + slot);
        }
    }

    private static OPanelInventoryType toCommonType(InventoryType inventoryType) {
        return switch(inventoryType) {
            case MAIN -> OPanelInventoryType.MAIN;
            case EQUIPMENTS -> OPanelInventoryType.EQUIPMENTS;
            case ENDER_CHEST -> OPanelInventoryType.ENDER_CHEST;
        };
    }

    private static InventoryItem toPublicItem(OPanelInventory.OPanelItemStack item) {
        return new InventoryItem(item.slot(), item.id(), Math.max(0, item.count()), item.snbt());
    }

    private static OPanelInventory.OPanelItemStack toCommonItem(InventoryItem item) {
        return new OPanelInventory.OPanelItemStack(
                item.getSlot(),
                item.getId(),
                item.getCount(),
                item.getSnbt().orElse(null)
        );
    }
}
