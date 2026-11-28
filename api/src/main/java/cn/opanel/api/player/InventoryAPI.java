package cn.opanel.api.player;

import cn.opanel.api.exception.OperationFailedException;

import java.util.List;

/**
 * Inventory access for one player.
 *
 * <p>The handle is bound to a player UUID and resolves that player on every
 * operation. It supports both live and saved offline inventories when the
 * server platform provides them. Returned item objects and lists are immutable
 * snapshots. Player lookup and platform failures use the same exception model
 * as {@link PlayerAPI}.</p>
 *
 * <p>单个玩家的物品栏访问接口。</p>
 *
 * <p>该句柄绑定到一个玩家 UUID，并在每次操作时解析该玩家。当服务器平台
 * 提供时，它同时支持在线与已保存的离线物品栏。返回的物品对象和列表都是
 * 不可变的快照。玩家查找与平台失败使用与 {@link PlayerAPI} 相同的异常模型。</p>
 */
public interface InventoryAPI { // 单个玩家的物品栏访问 API
    /**
     * Returns the number of logical slots in an inventory section.
     *
     * @param inventoryType section to inspect
     * @return the number of addressable slots
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     */
    int getSize(InventoryType inventoryType); // 返回物品栏某个分区的逻辑槽位数量

    /**
     * Returns an immutable snapshot of all items in the main inventory.
     *
     * @return an unmodifiable list of item snapshots
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     */
    default List<InventoryItem> getItems() { // 返回主物品栏中所有物品的不可变快照
        return getItems(InventoryType.MAIN);
    }

    /**
     * Returns an immutable snapshot of all items in an inventory section.
     *
     * @param inventoryType section to inspect
     * @return an unmodifiable list of item snapshots
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     */
    List<InventoryItem> getItems(InventoryType inventoryType); // 返回物品栏某个分区中所有物品的不可变快照

    /**
     * Returns the item occupying one logical slot in the main inventory.
     * Empty slots are represented by an item whose ID is {@code minecraft:air}
     * and whose count is zero.
     *
     * @param slot zero-based logical slot index
     * @return an immutable item snapshot
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     * @throws OperationFailedException if {@code slot}
     *         is outside the selected section
     */
    default InventoryItem getItem(int slot) { // 返回主物品栏中某个逻辑槽位上的物品
        return getItem(InventoryType.MAIN, slot);
    }

    /**
     * Returns the item occupying one logical slot. Empty slots are represented by
     * an item whose ID is {@code minecraft:air} and whose count is zero.
     *
     * @param inventoryType section to inspect
     * @param slot zero-based logical slot index
     * @return an immutable item snapshot
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     * @throws OperationFailedException if {@code slot}
     *         is outside the selected section
     */
    InventoryItem getItem(InventoryType inventoryType, int slot); // 返回物品栏某个分区中指定逻辑槽位上的物品

    /**
     * Replaces one inventory section in the main inventory. This operation may
     * block and must not be called from an extension lifecycle callback or
     * the Minecraft main thread. Items omitted from the supplied list are cleared
     * from the section.
     *
     * @param items item snapshots to write; slot values must be unique and in range
     * @throws NullPointerException if {@code inventoryType} or {@code items} is {@code null}
     * @throws OperationFailedException if a list element
     *         is {@code null}, a slot is out of range or duplicated, or an item
     *         cannot be decoded by the platform
     */
    default void setItems(List<InventoryItem> items) { // 替换主物品栏中的某个分区
        setItems(InventoryType.MAIN, items);
    }

    /**
     * Replaces one inventory section. This operation may block and must not be
     * called from an extension lifecycle callback or the Minecraft main thread.
     * Items omitted from the supplied list are cleared from the section.
     *
     * @param inventoryType section to replace
     * @param items item snapshots to write; slot values must be unique and in range
     * @throws NullPointerException if {@code inventoryType} or {@code items} is {@code null}
     * @throws OperationFailedException if a list element
     *         is {@code null}, a slot is out of range or duplicated, or an item
     *         cannot be decoded by the platform
     */
    void setItems(InventoryType inventoryType, List<InventoryItem> items); // 替换物品栏中的某个分区

    /**
     * Replaces one inventory slot in the main inventory. This operation may
     * block and must not be called from an extension lifecycle callback or
     * the Minecraft main thread. Passing an item with zero count or ID {@code minecraft:air}
     * clears the slot.
     *
     * @param item item snapshot to write
     * @throws NullPointerException if an argument is {@code null}
     * @throws OperationFailedException if the slot is
     *         out of range or the item cannot be decoded by the platform
     */
    default void setItem(InventoryItem item) { // 替换主物品栏中的某个槽位
        setItem(InventoryType.MAIN, item);
    }

    /**
     * Replaces one inventory slot. This operation may block and must not be
     * called from an extension lifecycle callback or the Minecraft main thread.
     * Passing an item with zero count or ID {@code minecraft:air} clears the slot.
     *
     * @param inventoryType section containing the slot
     * @param item item snapshot to write
     * @throws NullPointerException if an argument is {@code null}
     * @throws OperationFailedException if the slot is
     *         out of range or the item cannot be decoded by the platform
     */
    void setItem(InventoryType inventoryType, InventoryItem item); // 替换物品栏某个分区中的某个槽位
}
