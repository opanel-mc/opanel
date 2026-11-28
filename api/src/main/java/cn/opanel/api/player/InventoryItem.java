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
 *
 * <p>单个逻辑物品栏槽位的不可变表示。</p>
 *
 * <p>物品 ID 使用 Minecraft 命名空间形式，例如 {@code minecraft:diamond}。
 * 可选的 SNBT 包含平台无关的序列化物品数据/组件，用于保留名称、附魔、
 * 容器内容等元数据。</p>
 */
public final class InventoryItem { // 单个逻辑物品栏槽位的不可变表示
    private final int slot; // 零基逻辑槽位索引
    private final String id; // Minecraft 命名空间物品 ID
    private final int count; // 物品数量；零表示空堆叠
    private final String snbt; // 序列化的物品数据/组件，不存在时为 null

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
    public InventoryItem(int slot, String id, int count, String snbt) { // 创建物品栏物品值
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
    public int getSlot() { // 返回零基逻辑槽位索引
        return slot;
    }

    /**
     * @return the Minecraft namespaced item ID
     */
    public String getId() { // 返回 Minecraft 命名空间物品 ID
        return id;
    }

    /**
     * @return the number of items in the stack
     */
    public int getCount() { // 返回堆叠中的物品数量
        return count;
    }

    /**
     * @return serialized item data/components, or an empty value when absent
     */
    public Optional<String> getSnbt() { // 返回序列化的物品数据/组件，不存在时为空值
        return Optional.ofNullable(snbt);
    }

    /**
     * Tests whether this value represents an empty slot.
     *
     * @return {@code true} for a zero-count stack or {@code minecraft:air}
     */
    public boolean isEmpty() { // 判断该值是否表示空槽位
        return count <= 0 || id.equals("minecraft:air");
    }

    @Override
    public boolean equals(Object object) { // 按槽位、ID、数量与 SNBT 比较两个物品值
        if(this == object) return true;
        if(!(object instanceof InventoryItem)) return false;
        InventoryItem item = (InventoryItem) object;
        return slot == item.slot
                && count == item.count
                && id.equals(item.id)
                && Objects.equals(snbt, item.snbt);
    }

    @Override
    public int hashCode() { // 基于槽位、ID、数量与 SNBT 计算哈希码
        return Objects.hash(slot, id, count, snbt);
    }

    @Override
    public String toString() { // 返回包含槽位、ID 与数量的调试字符串
        return "InventoryItem{slot=" + slot + ", id='" + id + "', count=" + count + "}";
    }
}
