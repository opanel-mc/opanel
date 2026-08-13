package cn.opanel.api.player;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of one logical inventory slot.
 *
 * <p>The item ID uses the Minecraft namespaced form, for example
 * {@code minecraft:diamond}. Optional SNBT contains the platform-independent
 * serialized item data/components used to preserve metadata such as names,
 * enchantments, and container contents.</p>
 */
public final class InventoryItem {
    private final int slot;
    private final String id;
    private final int count;
    private final String snbt;

    /**
     * Creates an inventory item value.
     *
     * @param slot zero-based logical slot index
     * @param id Minecraft namespaced item ID
     * @param count item count; zero represents an empty stack
     * @param snbt serialized item data/components, or {@code null} when absent
     * @throws IllegalArgumentException if {@code slot} or {@code count} is negative
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public InventoryItem(int slot, String id, int count, String snbt) {
        if(slot < 0) throw new IllegalArgumentException("slot must not be negative");
        if(count < 0) throw new IllegalArgumentException("count must not be negative");
        this.slot = slot;
        this.id = Objects.requireNonNull(id, "id");
        this.count = count;
        this.snbt = snbt;
    }

    /**
     * @return the zero-based logical slot index
     */
    public int getSlot() {
        return slot;
    }

    /**
     * @return the Minecraft namespaced item ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return the number of items in the stack
     */
    public int getCount() {
        return count;
    }

    /**
     * @return serialized item data/components, or an empty value when absent
     */
    public Optional<String> getSnbt() {
        return Optional.ofNullable(snbt);
    }

    /**
     * Tests whether this value represents an empty slot.
     *
     * @return {@code true} for a zero-count stack or {@code minecraft:air}
     */
    public boolean isEmpty() {
        return count <= 0 || id.equals("minecraft:air");
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) return true;
        if(!(object instanceof InventoryItem)) return false;
        InventoryItem item = (InventoryItem) object;
        return slot == item.slot
                && count == item.count
                && id.equals(item.id)
                && Objects.equals(snbt, item.snbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, id, count, snbt);
    }

    @Override
    public String toString() {
        return "InventoryItem{slot=" + slot + ", id='" + id + "', count=" + count + "}";
    }
}
