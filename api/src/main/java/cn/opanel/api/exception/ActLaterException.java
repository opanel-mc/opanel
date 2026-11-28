package cn.opanel.api.exception;

/**
 * Signals deferred success: an operation was accepted but will be applied later,
 * normally during the next server restart.
 *
 * <p>This is distinct from {@link OperationFailedException}; callers should not
 * retry the operation immediately. It is currently used by plugin/mod state
 * changes on platforms that cannot safely change a loaded JAR in place.</p>
 *
 * <p>表示延迟成功：操作已被接受，但稍后才会生效，通常是在下次服务器重启时。</p>
 *
 * <p>这与 {@link OperationFailedException} 不同；调用方不应立即重试该操作。
 * 目前它用于那些无法安全地就地修改已加载 JAR 的平台上的插件/模组状态变更。</p>
 */
public class ActLaterException extends RuntimeException { // 延迟生效异常：操作已接受，将在稍后（通常为下次重启）应用
    public ActLaterException() { // 无参构造
        super();
    }
}
