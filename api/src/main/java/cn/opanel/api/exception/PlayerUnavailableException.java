package cn.opanel.api.exception;

import java.util.UUID;

/**
 * Thrown when a player handle can no longer resolve its player UUID through the
 * active server implementation.
 *
 * <p>当玩家句柄无法再通过当前激活的服务器实现解析其玩家 UUID 时抛出。</p>
 */
public final class PlayerUnavailableException extends OPanelAPIException { // 玩家不可用异常
    private final UUID uniqueId; // 无法解析的玩家 UUID

    /**
     * @param uniqueId UUID that could not be resolved
     * @param message failure description
     */
    public PlayerUnavailableException(UUID uniqueId, String message) { // 使用无法解析的 UUID 与失败描述构造异常
        super(message);
        this.uniqueId = uniqueId;
    }

    /**
     * @return the unresolved player UUID
     */
    public UUID getUniqueId() { // 返回无法解析的玩家 UUID
        return uniqueId;
    }
}
