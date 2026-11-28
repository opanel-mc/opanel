package cn.opanel.api.exception;

/**
 * Thrown when an extension uses an API handle after that extension was unloaded.
 *
 * <p>Every child API obtained from the root OPanel API shares the root handle's
 * lifetime and becomes unavailable at the same time.</p>
 *
 * <p>当扩展在自身被卸载之后仍使用 API 句柄时抛出。</p>
 *
 * <p>从根 OPanel API 获取的每个子 API 都与根句柄共享生命周期，
 * 并在同一时刻变为不可用。</p>
 */
public final class APIUnavailableException extends OPanelAPIException { // API 不可用异常
    public APIUnavailableException(String message) { // 使用描述性消息构造异常
        super(message);
    }
}
