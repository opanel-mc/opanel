package cn.opanel.api.event;

import cn.opanel.api.player.GameMode;
import cn.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when a player's game mode changes.
 *
 * <p>The event exposes the new game mode. The previous game mode is not
 * retained by the extension event API.</p>
 *
 * <p>当玩家的游戏模式发生变化时触发。</p>
 *
 * <p>该事件暴露新的游戏模式。扩展事件 API 不会保留之前的游戏模式。</p>
 */
public final class PlayerGameModeChangeEvent extends ExtensionEvent { // 玩家游戏模式变更事件
    private final PlayerAPI player; // 发生游戏模式变更的玩家
    private final GameMode gameMode; // 玩家的新游戏模式

    public PlayerGameModeChangeEvent(PlayerAPI player, GameMode gameMode) { // 构造事件，参数均不允许为 null
        this.player = Objects.requireNonNull(player, "player");
        this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
    }

    /**
     * @return the player whose game mode changed
     */
    public PlayerAPI getPlayer() { // 返回游戏模式发生变化的玩家
        return player;
    }

    /**
     * @return the player's new game mode
     */
    public GameMode getGameMode() { // 返回玩家的新游戏模式
        return gameMode;
    }
}
