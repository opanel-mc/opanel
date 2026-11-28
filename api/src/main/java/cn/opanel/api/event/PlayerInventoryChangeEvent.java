package cn.opanel.api.event;

import cn.opanel.api.player.InventoryAPI;
import cn.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when OPanel's inventory poller detects a player inventory change.
 *
 * <p>The player and inventory are live API handles rather than immutable
 * snapshots. The internal inventory hash used to detect changes is not exposed.
 * This event normally runs on OPanel's inventory poller thread.</p>
 *
 * <p>当 OPanel 的物品栏轮询器检测到玩家物品栏发生变化时触发。</p>
 *
 * <p>玩家和物品栏是实时的 API 句柄，而不是不可变的快照。用于检测变化的
 * 内部物品栏哈希值不会对外暴露。该事件通常在 OPanel 的物品栏轮询线程上运行。</p>
 */
public final class PlayerInventoryChangeEvent extends ExtensionEvent { // 玩家物品栏变更事件
    private final PlayerAPI player; // 物品栏发生变化的玩家
    private final InventoryAPI inventory; // 该玩家的实时物品栏句柄

    public PlayerInventoryChangeEvent(PlayerAPI player, InventoryAPI inventory) { // 构造事件，参数均不允许为 null
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /**
     * @return the player whose inventory changed
     */
    public PlayerAPI getPlayer() { // 返回物品栏发生变化的玩家
        return player;
    }

    /**
     * @return the live inventory handle for the player
     */
    public InventoryAPI getInventory() { // 返回该玩家的实时物品栏句柄
        return inventory;
    }
}
