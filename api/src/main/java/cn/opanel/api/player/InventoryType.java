package cn.opanel.api.player;

/**
 * Logical sections of a Minecraft player's inventory.
 *
 * <p>Minecraft 玩家物品栏的逻辑分区。</p>
 */
public enum InventoryType { // 玩家物品栏的逻辑分区
    /** Main inventory, including the hotbar. */
    MAIN("main"), // 主物品栏，包含快捷栏
    /** Armor slots and the off-hand slot. */
    EQUIPMENTS("equipments"), // 盔甲槽位与副手槽位
    /** The player's persistent ender chest. */
    ENDER_CHEST("enderChest"); // 玩家持久的末影箱

    private final String name; // OPanel 协议使用的稳定序列化名称

    InventoryType(String name) { // 使用序列化名称构造分区
        this.name = name;
    }

    /**
     * Returns the stable serialized name used by OPanel protocols.
     *
     * @return the serialized inventory type name
     */
    public String getName() { // 返回 OPanel 协议使用的稳定序列化名称
        return name;
    }
}
