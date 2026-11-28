package cn.opanel.api.exception;

/**
 * Base runtime exception for failures reported by the OPanel extension API.
 *
 * <p>Callers may catch this type for general API failures or catch a concrete
 * subtype when recovery depends on the failure category.</p>
 *
 * <p>OPanel 扩展 API 报告的失败的基类运行时异常。</p>
 *
 * <p>调用方可以捕获此类型来处理一般的 API 失败，也可以在恢复逻辑依赖于
 * 失败类别时捕获具体的子类型。</p>
 */
public abstract class OPanelAPIException extends RuntimeException { // OPanel 扩展 API 异常基类
    /**
     * Creates an API exception with a descriptive message.
     *
     * @param message failure description
     */
    protected OPanelAPIException(String message) { // 使用描述性消息创建 API 异常
        super(message);
    }

    /**
     * Creates an API exception with a descriptive message and original cause.
     *
     * @param message failure description
     * @param cause underlying failure
     */
    protected OPanelAPIException(String message, Throwable cause) { // 使用描述性消息和原始原因创建 API 异常
        super(message, cause);
    }
}
