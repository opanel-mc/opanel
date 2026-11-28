package cn.opanel.api.event;

import cn.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when OPanel detects that a player has moved.
 *
 * <p>This event does not contain old and new position snapshots. The player's
 * current position can be read through {@link PlayerAPI#getPosition()} while
 * handling the event. This is a high-frequency event, so handlers must remain
 * lightweight.</p>
 *
 * <p>当 OPanel 检测到玩家发生移动时触发。</p>
 *
 * <p>该事件不包含旧位置和新位置的快照。在处理事件期间，可以通过
 * {@link PlayerAPI#getPosition()} 读取玩家的当前位置。这是一个高频事件，
 * 因此处理器必须保持轻量。</p>
 */
public final class PlayerMoveEvent extends ExtensionEvent { // 玩家移动事件
    private final PlayerAPI player; // 位置发生变化的玩家

    public PlayerMoveEvent(PlayerAPI player) { // 构造事件，参数不允许为 null
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return the player whose position changed
     */
    public PlayerAPI getPlayer() { // 返回位置发生变化的玩家
        return player;
    }
}
