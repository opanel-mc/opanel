package cn.opanel.api.exception;

import java.util.Objects;

/**
 * Wraps an unexpected platform, I/O, validation, or implementation failure that
 * occurred while executing a named extension API operation.
 *
 * <p>The original exception is available through {@link #getCause()}.</p>
 *
 * <p>包装在执行某个具名的扩展 API 操作期间发生的意外平台、I/O、
 * 校验或实现失败。</p>
 *
 * <p>原始异常可以通过 {@link #getCause()} 获取。</p>
 */
public final class OperationFailedException extends OPanelAPIException { // 操作失败异常
    private final String operation; // 操作描述

    /**
     * Creates a wrapped operation failure.
     *
     * @param operation short operation description, such as {@code get player}
     * @param cause underlying failure
     * @throws NullPointerException if an argument is {@code null}
     */
    public OperationFailedException(String operation, Throwable cause) { // 创建包装后的操作失败异常
        super("Failed to " + Objects.requireNonNull(operation, "operation") + ".", Objects.requireNonNull(cause, "cause"));
        this.operation = operation;
    }

    /**
     * @return the operation description supplied when the exception was created
     */
    public String getOperation() { // 返回创建异常时提供的操作描述
        return operation;
    }
}
