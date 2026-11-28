package cn.opanel.api.event;

import cn.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired after a player joins the server.
 *
 * <p>The player is a live API handle rather than an immutable snapshot. The
 * handle remains usable while the owning extension is active and subject to
 * the normal {@link PlayerAPI} availability rules.</p>
 *
 * <p>在玩家加入服务器之后触发。</p>
 *
 * <p>玩家是实时的 API 句柄，而不是不可变的快照。只要所属扩展处于激活状态，
 * 该句柄就可继续使用，并遵循 {@link PlayerAPI} 的常规可用性规则。</p>
 */
public final class PlayerJoinEvent extends ExtensionEvent { // 玩家加入服务器事件
    private final PlayerAPI player; // 加入服务器的玩家

    public PlayerJoinEvent(PlayerAPI player) { // 构造事件，参数不允许为 null
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return the player that joined
     */
    public PlayerAPI getPlayer() { // 返回加入服务器的玩家
        return player;
    }
}
