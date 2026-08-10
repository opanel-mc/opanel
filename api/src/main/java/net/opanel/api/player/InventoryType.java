package net.opanel.api.player;

/**
 * Logical sections of a Minecraft player's inventory.
 */
public enum InventoryType {
    /** Main inventory, including the hotbar. */
    MAIN("main"),
    /** Armor slots and the off-hand slot. */
    EQUIPMENTS("equipments"),
    /** The player's persistent ender chest. */
    ENDER_CHEST("enderChest");

    private final String name;

    InventoryType(String name) {
        this.name = name;
    }

    /**
     * Returns the stable serialized name used by OPanel protocols.
     *
     * @return the serialized inventory type name
     */
    public String getName() {
        return name;
    }
}
