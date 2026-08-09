package net.opanel.api.player;

import java.util.List;

/**
 * Inventory access for one player.
 *
 * <p>The handle is bound to a player UUID and resolves that player on every
 * operation. It supports both live and saved offline inventories when the
 * server platform provides them. Returned item objects and lists are immutable
 * snapshots. Player lookup and platform failures use the same exception model
 * as {@link PlayerAPI}.</p>
 */
public interface InventoryAPI {
    /**
     * Returns the number of logical slots in an inventory section.
     *
     * @param inventoryType section to inspect
     * @return the number of addressable slots
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     */
    int getSize(InventoryType inventoryType);

    /**
     * Returns an immutable snapshot of all items in an inventory section.
     *
     * @param inventoryType section to inspect
     * @return an unmodifiable list of item snapshots
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     */
    List<InventoryItem> getItems(InventoryType inventoryType);

    /**
     * Returns the item occupying one logical slot. Empty slots are represented by
     * an item whose ID is {@code minecraft:air} and whose count is zero.
     *
     * @param inventoryType section to inspect
     * @param slot zero-based logical slot index
     * @return an immutable item snapshot
     * @throws NullPointerException if {@code inventoryType} is {@code null}
     * @throws net.opanel.api.exception.OperationFailedException if {@code slot}
     *         is outside the selected section
     */
    InventoryItem getItem(InventoryType inventoryType, int slot);

    /**
     * Replaces one inventory section. This operation may block and must not be
     * called from an extension lifecycle callback or the Minecraft main thread.
     * Items omitted from the supplied list are cleared from the section.
     *
     * @param inventoryType section to replace
     * @param items item snapshots to write; slot values must be unique and in range
     * @throws NullPointerException if {@code inventoryType} or {@code items} is {@code null}
     * @throws net.opanel.api.exception.OperationFailedException if a list element
     *         is {@code null}, a slot is out of range or duplicated, or an item
     *         cannot be decoded by the platform
     */
    void setItems(InventoryType inventoryType, List<InventoryItem> items);

    /**
     * Replaces one inventory slot. This operation may block and must not be
     * called from an extension lifecycle callback or the Minecraft main thread.
     * Passing an item with zero count or ID {@code minecraft:air} clears the slot.
     *
     * @param inventoryType section containing the slot
     * @param item item snapshot to write
     * @throws NullPointerException if an argument is {@code null}
     * @throws net.opanel.api.exception.OperationFailedException if the slot is
     *         out of range or the item cannot be decoded by the platform
     */
    void setItem(InventoryType inventoryType, InventoryItem item);
}
