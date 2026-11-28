package cn.opanel.api.event;

import cn.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when a player leaves the server.
 *
 * <p>The player is a live API handle rather than an immutable snapshot.
 * Availability of online-only data follows the normal {@link PlayerAPI}
 * rules for an offline player.</p>
 *
 * <p>当玩家离开服务器时触发。</p>
 *
 * <p>玩家是实时的 API 句柄，而不是不可变的快照。仅在线可用的数据的可用性
 * 遵循 {@link PlayerAPI} 对离线玩家的常规规则。</p>
 */
public final class PlayerLeaveEvent extends ExtensionEvent { // 玩家离开服务器事件
    private final PlayerAPI player; // 离开服务器的玩家

    public PlayerLeaveEvent(PlayerAPI player) { // 构造事件，参数不允许为 null
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return the player that left
     */
    public PlayerAPI getPlayer() { // 返回离开服务器的玩家
        return player;
    }
}
