package cn.opanel.api.exception;

import java.util.UUID;

/**
 * Thrown when a player exists but its current state does not permit an operation,
 * for example when attempting to kick an offline player.
 *
 * <p>当玩家存在但其当前状态不允许执行某项操作时抛出，
 * 例如尝试踢出（kick）一名离线玩家。</p>
 */
public final class InvalidPlayerStateException extends OPanelAPIException { // 玩家状态无效异常
    private final UUID uniqueId; // 处于无效状态的玩家的 UUID

    /**
     * @param uniqueId UUID of the player in the invalid state
     * @param message failure description
     */
    public InvalidPlayerStateException(UUID uniqueId, String message) { // 使用玩家 UUID 与失败描述构造异常
        super(message);
        this.uniqueId = uniqueId;
    }

    /**
     * @return UUID of the affected player
     */
    public UUID getUniqueId() { // 返回受影响玩家的 UUID
        return uniqueId;
    }
}
